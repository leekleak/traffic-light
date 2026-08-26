package com.leekleak.trafficlight.database

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-function tests for the counter-delta -> speed arithmetic used by
 * [TrafficSnapshot]. No Android framework dependency, so these run as plain
 * JVM unit tests without Robolectric.
 */
class BytesPerSecondTest {

    @Test
    fun `normal case divides delta by actual elapsed time`() {
        // previous RX = 1000, current RX = 3000, elapsed = 1s -> 2000 bytes/sec
        val speed = bytesPerSecond(
            currentBytes = 3000L, lastBytes = 1000L,
            currentSourceKey = "wlan0", lastSourceKey = "wlan0",
            currentElapsedRealtimeMs = 1000L, lastElapsedRealtimeMs = 0L,
        )
        assertEquals(2000L, speed)
    }

    @Test
    fun `long sampling interval uses actual elapsed time, not an assumed one`() {
        // 1830ms elapsed instead of the "expected" 1000ms.
        val speed = bytesPerSecond(
            currentBytes = 1830L, lastBytes = 0L,
            currentSourceKey = "wlan0", lastSourceKey = "wlan0",
            currentElapsedRealtimeMs = 1830L, lastElapsedRealtimeMs = 0L,
        )
        assertEquals(1000L, speed)
    }

    @Test
    fun `first sample has no previous source and produces no spike`() {
        val speed = bytesPerSecond(
            currentBytes = 5_000_000L, lastBytes = 0L,
            currentSourceKey = "wlan0", lastSourceKey = "", // no prior sample yet
            currentElapsedRealtimeMs = 1000L, lastElapsedRealtimeMs = 0L,
        )
        assertEquals(0L, speed)
    }

    @Test
    fun `changing measurement source rebaselines instead of diffing unrelated counters`() {
        // e.g. Wi-Fi (10 MB) -> Ethernet (50 GB): must not be read as ~50GB of traffic.
        val speed = bytesPerSecond(
            currentBytes = 50_000_000_000L, lastBytes = 10_000_000L,
            currentSourceKey = "eth0", lastSourceKey = "wlan0",
            currentElapsedRealtimeMs = 1000L, lastElapsedRealtimeMs = 0L,
        )
        assertEquals(0L, speed)
    }

    @Test
    fun `counter reset does not produce a negative or overflow-derived speed`() {
        // e.g. 10 GB -> 100 MB on the same interface (stats reset without reboot).
        val speed = bytesPerSecond(
            currentBytes = 100_000_000L, lastBytes = 10_000_000_000L,
            currentSourceKey = "wlan0", lastSourceKey = "wlan0",
            currentElapsedRealtimeMs = 1000L, lastElapsedRealtimeMs = 0L,
        )
        assertEquals(0L, speed)
    }

    @Test
    fun `zero traffic produces zero speed`() {
        val speed = bytesPerSecond(
            currentBytes = 42L, lastBytes = 42L,
            currentSourceKey = "wlan0", lastSourceKey = "wlan0",
            currentElapsedRealtimeMs = 1000L, lastElapsedRealtimeMs = 0L,
        )
        assertEquals(0L, speed)
    }

    @Test
    fun `very high throughput does not overflow`() {
        val tenGigabitPerSecondBytes = 1_250_000_000L
        val speed = bytesPerSecond(
            currentBytes = tenGigabitPerSecondBytes, lastBytes = 0L,
            currentSourceKey = "eth0", lastSourceKey = "eth0",
            currentElapsedRealtimeMs = 1000L, lastElapsedRealtimeMs = 0L,
        )
        assertEquals(tenGigabitPerSecondBytes, speed)
    }

    @Test
    fun `zero elapsed time is clamped instead of dividing by zero`() {
        val speed = bytesPerSecond(
            currentBytes = 100L, lastBytes = 0L,
            currentSourceKey = "wlan0", lastSourceKey = "wlan0",
            currentElapsedRealtimeMs = 500L, lastElapsedRealtimeMs = 500L,
        )
        // Elapsed clamped to 1ms -> 100 bytes / 1ms = 100_000 bytes/sec; the point of
        // this test is only that it returns a finite value instead of crashing.
        assertEquals(100_000L, speed)
    }

    @Test
    fun `upload and download are computed independently`() {
        val down = bytesPerSecond(2000L, 1000L, "wlan0", "wlan0", 1000L, 0L)
        val up = bytesPerSecond(1300L, 1000L, "wlan0", "wlan0", 1000L, 0L)
        assertEquals(1000L, down)
        assertEquals(300L, up)
    }
}
