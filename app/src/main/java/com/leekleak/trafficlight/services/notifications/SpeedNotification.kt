package com.leekleak.trafficlight.services.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.DeadSystemException
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.leekleak.trafficlight.MainActivity
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.TrafficSnapshot
import com.leekleak.trafficlight.database.TrafficSnapshotManager
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.model.PermissionManager
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.clipAndPad
import com.leekleak.trafficlight.util.toKb
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import timber.log.Timber
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

class SpeedNotification(
    @InjectedParam serviceScope: LifecycleCoroutineScope,
    @InjectedParam notificationId: Int,
    context: Context,
    notificationManager: NotificationManager,
    private val networkUsageManager: NetworkUsageManager,
    private val connectivityManager: ConnectivityManager,
    private val appPreferenceRepo: AppPreferenceRepo,
    private val trafficSnapshotManager: TrafficSnapshotManager,
    private val permissionManager: PermissionManager,
) : PersistentNotification(serviceScope, context, notificationManager, notificationId) {

    private var notificationBuilderSilent = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_SILENT)
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

    @Volatile private var speedSnapshot: TrafficSnapshot = TrafficSnapshot(0, 0, emptySet())

    init {
        scope.launch {
            appPreferenceRepo.modeAOD.collect { aodMode = it }
        }
        scope.launch {
            appPreferenceRepo.speedBits.collect { inBits = it; updateNotification(true) }
        }
        scope.launch {
            appPreferenceRepo.separateUpDown.collect { separateUpDown = it; updateNotification(true) }
        }
        scope.launch {
            appPreferenceRepo.liveNotification.collect { liveNotification = it; updateNotification(true) }
        }
        scope.launch {
            appPreferenceRepo.speedThreshold.collect { speedThreshold = it; updateNotification(true) }
        }
        scope.launch {
            appPreferenceRepo.speedThresholdKb.collect { speedThresholdKb = it; updateNotification(true) }
        }
        scope.launch {
            appPreferenceRepo.speedMetric.collect { speedMetric = it; updateNotification(true) }
        }
        scope.launch {
            appPreferenceRepo.sizeMetric.collect { sizeMetric = it; updateNotification(true) }
        }
        updateBaseNotification()
    }

    override fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            var lastSnapshot: TrafficSnapshot
            var currentSnapshot = trafficSnapshotManager.getTrafficSnapshot()

            while (true) {
                Timber.i("Updating notification")
                lastSnapshot = currentSnapshot
                currentSnapshot = trafficSnapshotManager.getTrafficSnapshot()

                /**
                 * If network speed is changing rapidly, we use this while loop to self-calibrate
                 * the refresh timing to match the timing of the TrafficStats API updates.
                 *
                 * If network speed is not changing rapidly (i.e. it's zero)
                 * it's quite likely that the next tick will also be zero, so we ignore that and
                 * simply sleep for 1 second
                 *
                 * Generally though, the system updates the counter once a second, so 100ms wait hits
                 * pretty much all the time, except when it's gone out of sync.
                 */
                if (lastSnapshot.total == currentSnapshot.total) {
                    Timber.i("Waiting for notification update")
                    delay(100.milliseconds)
                    currentSnapshot = trafficSnapshotManager.getTrafficSnapshot()
                }

                if (updateCounter >= DATA_UPDATE_FREQ) {
                    updateTodayUsage()
                    updateCounter = 0
                } else {
                    updateCounter++
                }

                speedSnapshot = (currentSnapshot - lastSnapshot)

                updateNotification()
                delay(900.milliseconds)
            }
        }

    }

    override fun cancel() {
        trafficSnapshotManager.close()
        super.cancel()
    }

    override fun screenStateChange(on: Boolean) {
        if (on) start()
        else if (!aodMode) job?.cancel()
    }

    private var lastTitle: String = ""
    private var lastContent: String = ""
    private suspend fun updateNotification(force: Boolean = false) {
        val trafficSnapshot: TrafficSnapshot = speedSnapshot
        val data = DataSize(trafficSnapshot.total).toString(speed = true, inBits = inBits, metric = speedMetric)
        val upload = DataSize(trafficSnapshot.up).toString(speed = true, inBits = inBits, metric = speedMetric)
        val download = DataSize(trafficSnapshot.down).toString(speed = true, inBits = inBits, metric = speedMetric)
        val title = context.getString(R.string.up_down, upload, download)

        val spacing = 18
        val messageShort =
            context.getString(R.string.wi_fi, DataSize(todayUsage.usage2).toString(metric = sizeMetric)).clipAndPad(spacing) +
            context.getString(R.string.mobile, DataSize(todayUsage.usage1).toString(metric = sizeMetric))

        val silent = shouldGoSilent()
        if (!force) updateSilentTicks()
        // Cancel update only if there is no need to update the notification channel
        if (lastTitle == data && lastContent == messageShort && silent == shouldGoSilent() && !force) return

        lastTitle = data
        lastContent = messageShort

        val speed = data.substringBefore(" ")
        val unit = data.substringAfter(" ")
        notification = (
                if (shouldGoSilent()) {
                    if (liveNotification) {
                        buildForChannel(NOTIFICATION_CHANNEL_ID_SILENT)
                    } else {
                        notificationBuilderSilent
                    }
                } else {
                    if (liveNotification) {
                        buildForChannel(NOTIFICATION_CHANNEL_ID)
                    } else {
                        notificationBuilder
                    }
                }
            )
            .setRequestPromotedOngoing(liveNotification && !shouldGoSilent())
            .apply {
                if (!liveNotification) {
                    setSmallIcon(
                        if (!separateUpDown) {
                            notificationIconHelper.createIcon(speed, unit)
                        } else {
                            val speedUp = DataSize(trafficSnapshot.up).toStringParts(inBits = inBits, metric = speedMetric)
                            val speedDown = DataSize(trafficSnapshot.down).toStringParts(inBits = inBits, metric = speedMetric)
                            notificationIconHelper.createIconSeparate(
                                speed1 = "${speedUp.first} ${speedUp.third.substring(0,1)}",
                                speed2 = "${speedDown.first} ${speedDown.third.substring(0,1)}"
                            )
                        }
                    )
                    setShowWhen(false)
                    setWhen(System.currentTimeMillis() + DECADE) // Used Long.MAX_VALUE before, but that caused overflows on honor android skins for reasons unknown
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

    private fun buildForChannel(channel: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channel).apply {
            setSmallIcon(R.drawable.notification)
            setContentTitle(context.getString(R.string.app_name_short))
            setOngoing(true)
            setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            setSilent(true)
            setLocalOnly(true)
            setOnlyAlertOnce(true)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            try {
                setContentIntent(
                    PendingIntent.getActivity(
                        context, 0, Intent(context, MainActivity::class.java).apply {
                            flags =
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }, PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } catch (_: DeadSystemException) {
                Timber.e("System died lol. Good luck!")
                cancel()
            }
        }
    }

    private fun updateBaseNotification() {
        notificationBuilder = buildForChannel(NOTIFICATION_CHANNEL_ID)
        notificationBuilderSilent = buildForChannel(NOTIFICATION_CHANNEL_ID_SILENT)

        notification = notificationBuilder.build()
    }

    private var silentChannelTicks: Long = 0
    private fun updateSilentTicks() {
        if ((speedThreshold &&
            (
                ((speedThresholdKb == -1L) && !isNetworkAvailable()) ||
                (speedSnapshot.total.toKb < speedThresholdKb)
            )
        )){
            silentChannelTicks++
        } else {
            silentChannelTicks = 0
        }
    }
    private fun shouldGoSilent(): Boolean = (silentChannelTicks >= SILENT_CHANNEL_TICK_TARGET) && speedThreshold

    private fun isNetworkAvailable(): Boolean {
        if (!permissionManager.internetPermission.value) return true
        return connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.run {
            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } ?: false
    }

    companion object {
        private const val DATA_UPDATE_FREQ = 4
        private const val SILENT_CHANNEL_TICK_TARGET = 4
        const val NOTIFICATION_CHANNEL_ID = "Persistent Notification"
        const val NOTIFICATION_CHANNEL_ID_SILENT = "Persistent Notification Silent"
    }
}