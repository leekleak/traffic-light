package com.leekleak.trafficlight.database

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/**
 * Covers the two failure modes the original implementation was prone to:
 * counting VPN/tunnel traffic on top of the physical interface it rides on
 * (double counting -> inflated speed), and treating a change of measurement
 * source (interface handover, or a newly appearing/disappearing counter) as
 * if it were real traffic (a spike).
 *
 * [BytesPerSecondTest] covers the counter-delta arithmetic directly with no
 * Android/Robolectric dependency. This class instead drives
 * [TrafficSnapshot] through its real public API with a mocked
 * [ConnectivityManager], to check the interface discovery and VPN exclusion
 * that arithmetic relies on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrafficSnapshotTest {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var appPreferenceRepo: AppPreferenceRepo

    @Before
    fun setUp() {
        mockkStatic(TrafficStats::class)
        every { TrafficStats.getTotalTxBytes() } returns 0L
        every { TrafficStats.getTotalRxBytes() } returns 0L

        connectivityManager = mockk()
        appPreferenceRepo = mockk(relaxed = true)
        every { appPreferenceRepo.forceFallback } returns flowOf(false)
    }

    @After
    fun tearDown() {
        unmockkStatic(TrafficStats::class)
    }

    private fun networkWith(interfaceName: String, vararg transports: Int): Network {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()
        val transportSet = transports.toSet()
        every { capabilities.hasTransport(any()) } answers { transportSet.contains(firstArg<Int>()) }
        val linkProperties = mockk<LinkProperties>()
        every { linkProperties.interfaceName } returns interfaceName
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { connectivityManager.getLinkProperties(network) } returns linkProperties
        return network
    }

    @Test
    fun `VPN interface traffic is excluded from the physical interface sum`() = runTest {
        val wifi = networkWith("wlan0", NetworkCapabilities.TRANSPORT_WIFI)
        val vpn = networkWith("tun0", NetworkCapabilities.TRANSPORT_VPN)
        every { connectivityManager.allNetworks } returns arrayOf(wifi, vpn)

        // wlan0 carries the (larger, encrypted) physical traffic; tun0 carries the
        // same logical traffic again, decrypted. Only wlan0 should be counted.
        every { TrafficStats.getRxBytes("wlan0") } returnsMany listOf(1_000_000L, 1_200_000L)
        every { TrafficStats.getTxBytes("wlan0") } returnsMany listOf(500_000L, 600_000L)
        every { TrafficStats.getRxBytes("tun0") } returnsMany listOf(950_000L, 1_140_000L)
        every { TrafficStats.getTxBytes("tun0") } returnsMany listOf(480_000L, 570_000L)

        val snapshot = TrafficSnapshot(this, appPreferenceRepo, connectivityManager)
        snapshot.updateSnapshot()
        snapshot.setCurrentAsLast()

        ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
        snapshot.updateSnapshot()

        // wlan0-only delta: +200,000 down, +100,000 up over 1s.
        // If tun0 had leaked into the sum this would be 390,000 / 190,000 instead.
        assertEquals(200_000L, snapshot.downSpeed)
        assertEquals(100_000L, snapshot.upSpeed)
    }

    @Test
    fun `switching physical interface rebaselines instead of spiking`() = runTest {
        val wifi = networkWith("wlan0", NetworkCapabilities.TRANSPORT_WIFI)
        val cellular = networkWith("rmnet0", NetworkCapabilities.TRANSPORT_CELLULAR)

        every { TrafficStats.getRxBytes("wlan0") } returns 10_000_000L
        every { TrafficStats.getTxBytes("wlan0") } returns 1_000_000L
        every { TrafficStats.getRxBytes("rmnet0") } returns 50_000_000_000L
        every { TrafficStats.getTxBytes("rmnet0") } returns 5_000_000_000L

        val snapshot = TrafficSnapshot(this, appPreferenceRepo, connectivityManager)

        every { connectivityManager.allNetworks } returns arrayOf(wifi)
        snapshot.updateSnapshot()
        snapshot.setCurrentAsLast()

        // Wi-Fi disappears, mobile (with an unrelated, much larger cumulative
        // counter) takes over as the only physical network.
        every { connectivityManager.allNetworks } returns arrayOf(cellular)
        ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
        snapshot.updateSnapshot()

        // Must not report ~50GB-10MB worth of "speed" from the handover.
        assertEquals(0L, snapshot.downSpeed)
        assertEquals(0L, snapshot.upSpeed)
    }

    @Test
    fun `no physical network falls back to device totals rather than crashing`() = runTest {
        every { connectivityManager.allNetworks } returns arrayOf()
        // Constant regardless of call count, so this stays deterministic no matter
        // how many times the flow collector in TrafficSnapshot's init block reads it.
        every { TrafficStats.getTotalTxBytes() } returns 0L
        every { TrafficStats.getTotalRxBytes() } returnsMany listOf(2_000L, 2_500L)

        val snapshot = TrafficSnapshot(this, appPreferenceRepo, connectivityManager)
        snapshot.updateSnapshot()
        snapshot.setCurrentAsLast()

        ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
        snapshot.updateSnapshot()

        assertEquals(500L, snapshot.downSpeed)
        assertEquals(0L, snapshot.upSpeed)
    }
}
