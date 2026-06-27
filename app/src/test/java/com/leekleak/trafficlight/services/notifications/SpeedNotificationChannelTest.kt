package com.leekleak.trafficlight.services.notifications

import com.leekleak.trafficlight.services.notifications.SpeedNotification.Companion.ChannelDecision
import com.leekleak.trafficlight.services.notifications.SpeedNotification.Companion.NOTIFICATION_CHANNEL_ID
import com.leekleak.trafficlight.services.notifications.SpeedNotification.Companion.NOTIFICATION_CHANNEL_ID_SILENT
import com.leekleak.trafficlight.services.notifications.SpeedNotification.Companion.chooseChannel
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [SpeedNotification.chooseChannel], the hysteresis-aware channel selector that
 * fixes the speed-notification "blinking" between the loud and silent channels reported in
 * issue #181 (a regression from PR #177).
 *
 * The helper is pure, so these tests drive it directly without constructing a SpeedNotification
 * or any Android framework objects.
 */
class SpeedNotificationChannelTest {

    private val ticks = 3

    /** Convenience: run one tick, returning the resulting decision. */
    private fun tick(
        instantaneousSilent: Boolean,
        prev: ChannelDecision,
        thresholdEnabled: Boolean = true,
    ): ChannelDecision = chooseChannel(
        speedThresholdEnabled = thresholdEnabled,
        instantaneousSilent = instantaneousSilent,
        currentChannel = prev.channel,
        streakChannel = prev.streakChannel,
        streak = prev.streak,
        hysteresisTicks = ticks,
    )

    private fun firstPost(instantaneousSilent: Boolean, thresholdEnabled: Boolean = true) =
        chooseChannel(
            speedThresholdEnabled = thresholdEnabled,
            instantaneousSilent = instantaneousSilent,
            currentChannel = null,
            streakChannel = null,
            streak = 0,
            hysteresisTicks = ticks,
        )

    @Test
    fun `steady above threshold holds the loud channel`() {
        var d = firstPost(instantaneousSilent = false)
        assertEquals(NOTIFICATION_CHANNEL_ID, d.channel)
        repeat(10) {
            d = tick(instantaneousSilent = false, prev = d)
            assertEquals(NOTIFICATION_CHANNEL_ID, d.channel)
        }
    }

    @Test
    fun `steady below threshold holds the silent channel`() {
        var d = firstPost(instantaneousSilent = true)
        assertEquals(NOTIFICATION_CHANNEL_ID_SILENT, d.channel)
        repeat(10) {
            d = tick(instantaneousSilent = true, prev = d)
            assertEquals(NOTIFICATION_CHANNEL_ID_SILENT, d.channel)
        }
    }

    @Test
    fun `tick-by-tick oscillation around the threshold does not flap the channel`() {
        // Start steadily on the loud channel.
        var d = firstPost(instantaneousSilent = false)
        assertEquals(NOTIFICATION_CHANNEL_ID, d.channel)

        // Now alternate below/above every tick (the regression scenario). Because the silent
        // preference never persists for `ticks` consecutive ticks, the channel must never move.
        var silent = true
        repeat(20) {
            d = tick(instantaneousSilent = silent, prev = d)
            assertEquals(
                "channel must not change while oscillating around the threshold",
                NOTIFICATION_CHANNEL_ID,
                d.channel,
            )
            silent = !silent
        }
    }

    @Test
    fun `channel switches only after the preference persists for the full debounce window`() {
        var d = firstPost(instantaneousSilent = false)
        assertEquals(NOTIFICATION_CHANNEL_ID, d.channel)

        // Two consecutive silent ticks: not enough yet, still loud.
        d = tick(instantaneousSilent = true, prev = d)
        assertEquals(NOTIFICATION_CHANNEL_ID, d.channel)
        d = tick(instantaneousSilent = true, prev = d)
        assertEquals(NOTIFICATION_CHANNEL_ID, d.channel)

        // Third consecutive silent tick reaches the window: switch to silent.
        d = tick(instantaneousSilent = true, prev = d)
        assertEquals(NOTIFICATION_CHANNEL_ID_SILENT, d.channel)
    }

    @Test
    fun `an interrupting opposite sample resets the debounce streak`() {
        var d = firstPost(instantaneousSilent = false)

        // Two silent, then one loud interrupts -> streak resets, still loud.
        d = tick(instantaneousSilent = true, prev = d)
        d = tick(instantaneousSilent = true, prev = d)
        d = tick(instantaneousSilent = false, prev = d)
        assertEquals(NOTIFICATION_CHANNEL_ID, d.channel)

        // Need a fresh run of `ticks` silent samples to switch.
        d = tick(instantaneousSilent = true, prev = d)
        d = tick(instantaneousSilent = true, prev = d)
        assertEquals(NOTIFICATION_CHANNEL_ID, d.channel)
        d = tick(instantaneousSilent = true, prev = d)
        assertEquals(NOTIFICATION_CHANNEL_ID_SILENT, d.channel)
    }

    @Test
    fun `single transient sample does not flip the channel (disconnected -1L case)`() {
        // Models speedThresholdKb == -1L: instantaneousSilent reflects "no network available".
        var d = firstPost(instantaneousSilent = false) // network available -> loud
        assertEquals(NOTIFICATION_CHANNEL_ID, d.channel)

        // A single transient "unavailable" sample must not immediately flip to silent.
        d = tick(instantaneousSilent = true, prev = d)
        assertEquals(NOTIFICATION_CHANNEL_ID, d.channel)

        // Recovering immediately keeps it loud.
        d = tick(instantaneousSilent = false, prev = d)
        assertEquals(NOTIFICATION_CHANNEL_ID, d.channel)
    }

    @Test
    fun `threshold disabled always selects the loud channel and never engages hysteresis`() {
        var d = firstPost(instantaneousSilent = true, thresholdEnabled = false)
        assertEquals(NOTIFICATION_CHANNEL_ID, d.channel)
        assertEquals(0, d.streak)

        repeat(5) {
            d = tick(instantaneousSilent = true, prev = d, thresholdEnabled = false)
            assertEquals(NOTIFICATION_CHANNEL_ID, d.channel)
            assertEquals(0, d.streak)
        }
    }

    @Test
    fun `first post chooses the correct channel without an extra flip`() {
        assertEquals(NOTIFICATION_CHANNEL_ID_SILENT, firstPost(instantaneousSilent = true).channel)
        assertEquals(NOTIFICATION_CHANNEL_ID, firstPost(instantaneousSilent = false).channel)
    }
}
