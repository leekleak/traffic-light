package com.leekleak.trafficlight.services.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.app.NotificationCompat
import com.leekleak.trafficlight.MainActivity
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.TrafficSnapshot
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.clipAndPad
import com.leekleak.trafficlight.util.toKb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

class SpeedNotification(
    serviceScope: CoroutineScope,
    context: Context,
    notificationManager: NotificationManager,
    notificationId: Int,
    private val networkUsageManager: NetworkUsageManager,
    private val connectivityManager: ConnectivityManager,
    private val appPreferenceRepo: AppPreferenceRepo,
    private val trafficSnapshot: TrafficSnapshot,
) : PersistentNotification(serviceScope, context, notificationManager, notificationId) {
    private var updateCounter = Int.MAX_VALUE

    private val queryMobile = UsageQuery(dataType = DataType.Mobile)
    private val queryWifi = UsageQuery(dataType = DataType.Wifi)

    private var aodMode = false
    private var inBits = false
    private var separateUpDown = false
    private var liveNotification = false
    private var speedThreshold = false
    private var speedThresholdKb = -1L
    private var speedMetric = false
    private var sizeMetric = false
    private var todayUsage = DayUsage()

    init {
        scope.launch {
            appPreferenceRepo.modeAOD.collect { aodMode = it }
        }
        scope.launch {
            appPreferenceRepo.speedBits.collect { inBits = it; updateNotification(trafficSnapshot, true) }
        }
        scope.launch {
            appPreferenceRepo.separateUpDown.collect { separateUpDown = it; updateNotification(trafficSnapshot, true) }
        }
        scope.launch {
            appPreferenceRepo.liveNotification.collect { liveNotification = it; updateNotification(trafficSnapshot, true) }
        }
        scope.launch {
            appPreferenceRepo.speedThreshold.collect { speedThreshold = it; updateNotification(trafficSnapshot, true) }
        }
        scope.launch {
            appPreferenceRepo.speedThresholdKb.collect { speedThresholdKb = it; updateNotification(trafficSnapshot, true) }
        }
        scope.launch {
            appPreferenceRepo.speedMetric.collect { speedMetric = it; updateNotification(trafficSnapshot, true) }
        }
        scope.launch {
            appPreferenceRepo.sizeMetric.collect { sizeMetric = it; updateNotification(trafficSnapshot, true) }
        }
        updateBaseNotification()
    }

    override fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            trafficSnapshot.updateSnapshot()
            trafficSnapshot.setCurrentAsLast()
            while (true) {
                Timber.i("Updating notification")
                trafficSnapshot.updateSnapshot()

                /**
                 * If network speed is changing rapidly, we use this while loop to self-calibrate
                 * the refresh timing to match the timing of the TrafficStats API updates.
                 *
                 * If network speed is not changing rapidly (i.e. it's zero)
                 * it's quite likely that the next tick will also be zero, so we ignore that and
                 * simply sleep for 1 second
                 */
                if (trafficSnapshot.isCurrentSameAsLast()) {
                    delay(100.milliseconds)
                    trafficSnapshot.updateSnapshot()
                }

                if (updateCounter >= DATA_UPDATE_FREQ) {
                    updateTodayUsage()
                    updateCounter = 0
                } else {
                    updateCounter++
                }

                updateNotification(trafficSnapshot)
                trafficSnapshot.setCurrentAsLast()
                delay(900.milliseconds)
            }
        }

    }

    override fun screenStateChange(on: Boolean) {
        if (on) start()
        else if (!aodMode) job?.cancel()
    }

    private var lastTitle: String = ""
    private var lastContent: String = ""
    private suspend fun updateNotification(trafficSnapshot: TrafficSnapshot, force: Boolean = false) {
        val data = DataSize(trafficSnapshot.totalSpeed).toString(speed = true, inBits = inBits, metric = speedMetric)
        val download = DataSize(trafficSnapshot.downSpeed).toString(speed = true, inBits = inBits, metric = speedMetric)
        val upload = DataSize(trafficSnapshot.upSpeed).toString(speed = true, inBits = inBits, metric = speedMetric)
        val title = context.getString(R.string.up_down, upload, download)

        val spacing = 18
        val messageShort =
            context.getString(R.string.wi_fi, DataSize(todayUsage.usage2).toString(metric = sizeMetric)).clipAndPad(spacing) +
            context.getString(R.string.mobile, DataSize(todayUsage.usage1).toString(metric = sizeMetric))

        // Advance the channel hysteresis every tick, before the content early-return below.
        // Otherwise a steady speed that produces identical text would short-circuit and the
        // streak would never accumulate, leaving the notification stuck on the old channel
        // after a real threshold crossing. Re-post if the channel actually changed even when
        // the text is unchanged.
        val previousChannel = currentChannel
        resolveChannel(immediate = force)
        val channelChanged = currentChannel != previousChannel

        if (lastTitle == data && lastContent == messageShort && !channelChanged && !force) return
        lastTitle = data
        lastContent = messageShort

        val speed = data.substringBefore(" ")
        val unit = data.substringAfter(" ")
        updateBaseNotification()
        notification = notificationBuilder
            .apply {
                if (!liveNotification) {
                    setSmallIcon(
                        if (!separateUpDown) {
                            notificationIconHelper.createIcon(speed, unit)
                        } else {
                            val speedUp = DataSize(trafficSnapshot.upSpeed).toStringParts(inBits = inBits, metric = speedMetric)
                            val speedDown = DataSize(trafficSnapshot.downSpeed).toStringParts(inBits = inBits, metric = speedMetric)
                            notificationIconHelper.createIconSeparate(
                                speed1 = "${speedUp.first} ${speedUp.third.substring(0,1)}",
                                speed2 = "${speedDown.first} ${speedDown.third.substring(0,1)}"
                            )
                        })
                    setWhen(Long.MAX_VALUE) // Keep above other notifications
                    setShowWhen(false) // Hide timestamp
                }
                else  {
                    setSmallIcon(R.drawable.mobiledata_arrows)
                    setShortCriticalText(data)
                }
            }
            .setContentTitle(title)
            .setContentText(messageShort)
            .build()
        notifySafely(notificationId, notification)
    }

    private suspend fun updateTodayUsage() {
        val date = LocalDate.now()
        val mobile = networkUsageManager.totalDayUsage(queryMobile, date)
        val wifi = networkUsageManager.totalDayUsage(queryWifi, date)
        todayUsage = DayUsage(date, mobile, wifi)
    }

    // Retained channel-selection state for hysteresis (see chooseChannel). `currentChannel`
    // is the channel the notification is currently posted on (null before the first post);
    // `pendingChannel`/`pendingStreak` track how long the instantaneous decision has wanted
    // to switch away from `currentChannel`.
    private var currentChannel: String? = null
    private var pendingChannel: String? = null
    private var pendingStreak = 0

    /**
     * Advances the hysteresis state for one update tick and returns the channel the notification
     * should be posted on. This is the only place the streak state mutates, so it must be called
     * exactly once per update tick (see [updateNotification]). It is intentionally cheap (no
     * builder allocation) so it can run on every tick, including the ones whose textual content
     * is unchanged and would otherwise short-circuit before the notification is rebuilt.
     *
     * Pass [immediate] for forced updates (e.g. the user toggling `speedThreshold` or changing
     * `speedThresholdKb`): the new preference is adopted at once and the debounce is reset, so a
     * settings change takes effect immediately rather than after the hysteresis window. Hysteresis
     * is only meant to absorb tick-by-tick speed oscillation, not deliberate setting changes.
     */
    private fun resolveChannel(immediate: Boolean = false): String {
        // Instantaneous channel preference from the current sample, preserving the original
        // precedence: silent when the threshold feature is on AND either (the -1L "no network"
        // setting and no network is available) OR the current speed is below the threshold.
        val instantaneousSilent = speedThreshold &&
            (
                ((speedThresholdKb == -1L) && !isNetworkAvailable()) ||
                (trafficSnapshot.totalSpeed.toKb < speedThresholdKb)
            )

        // Passing currentChannel = null reuses chooseChannel's "first post" path, which adopts the
        // instantaneous preference directly and clears the streak.
        val decision = chooseChannel(
            speedThresholdEnabled = speedThreshold,
            instantaneousSilent = instantaneousSilent,
            currentChannel = if (immediate) null else currentChannel,
            streakChannel = pendingChannel,
            streak = pendingStreak,
            hysteresisTicks = CHANNEL_HYSTERESIS_TICKS,
        )
        currentChannel = decision.channel
        pendingChannel = decision.streakChannel
        pendingStreak = decision.streak
        return decision.channel
    }

    private fun updateBaseNotification() {
        // Use the channel already resolved for this tick; only resolve here for the very first
        // post (init), so the hysteresis streak is never advanced twice in a single tick.
        val channel = currentChannel ?: resolveChannel()
        notificationBuilder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(context.getString(R.string.app_name_short))
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setRequestPromotedOngoing(liveNotification)
            .setSilent(true)
            .setLocalOnly(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }, PendingIntent.FLAG_IMMUTABLE
                )
            )
        notification = notificationBuilder.build()
    }

    private fun isNetworkAvailable(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            capabilities?.run {
                hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } ?: false
        } else {
            connectivityManager.activeNetworkInfo?.run {
                when (type) {
                    ConnectivityManager.TYPE_WIFI -> true
                    ConnectivityManager.TYPE_MOBILE -> true
                    ConnectivityManager.TYPE_ETHERNET -> true
                    else -> false
                }
            } ?: false
        }
    }

    companion object {
        private const val DATA_UPDATE_FREQ = 4

        /**
         * Number of consecutive update ticks the instantaneous channel preference must disagree
         * with the currently-posted channel before the notification is actually moved. At ~900ms
         * per tick this debounces the loud/silent switch over a short window so that a speed
         * oscillating right around the threshold no longer re-posts (and visibly "blinks") the
         * notification every tick. See issue #181 (regression from #177).
         */
        const val CHANNEL_HYSTERESIS_TICKS = 3

        const val NOTIFICATION_CHANNEL_ID = "Persistent Notification"
        const val NOTIFICATION_CHANNEL_ID_SILENT = "Persistent Notification Silent"

        /**
         * Result of a hysteresis-aware channel decision: the channel to post on now, plus the
         * retained streak state to feed back into the next [chooseChannel] call.
         */
        data class ChannelDecision(
            val channel: String,
            val streakChannel: String?,
            val streak: Int,
        )

        /**
         * Pure channel-selection helper with hysteresis. Given the instantaneous channel
         * preference and the retained streak state, returns the channel to post on plus the
         * updated streak state.
         *
         * Rules:
         *  - threshold feature disabled -> always the loud channel, streak cleared.
         *  - first post (currentChannel == null) -> adopt the instantaneous preference immediately.
         *  - instantaneous preference matches the current channel -> hold it, streak cleared.
         *  - instantaneous preference differs -> count consecutive ticks; only switch once the
         *    preference has held for [hysteresisTicks] ticks, otherwise keep the current channel.
         */
        fun chooseChannel(
            speedThresholdEnabled: Boolean,
            instantaneousSilent: Boolean,
            currentChannel: String?,
            streakChannel: String?,
            streak: Int,
            hysteresisTicks: Int = CHANNEL_HYSTERESIS_TICKS,
        ): ChannelDecision {
            if (!speedThresholdEnabled) {
                return ChannelDecision(NOTIFICATION_CHANNEL_ID, null, 0)
            }

            val desired = if (instantaneousSilent) NOTIFICATION_CHANNEL_ID_SILENT else NOTIFICATION_CHANNEL_ID

            // First post: there is nothing to flap away from, so adopt the preference directly.
            if (currentChannel == null) {
                return ChannelDecision(desired, null, 0)
            }

            // Preference agrees with what is shown: nothing to switch, drop any pending streak.
            if (desired == currentChannel) {
                return ChannelDecision(currentChannel, null, 0)
            }

            // Preference wants to switch: require it to persist for `hysteresisTicks` ticks.
            val newStreak = if (streakChannel == desired) streak + 1 else 1
            return if (newStreak >= hysteresisTicks) {
                ChannelDecision(desired, null, 0)
            } else {
                ChannelDecision(currentChannel, desired, newStreak)
            }
        }
    }
}