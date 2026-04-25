package com.leekleak.trafficlight.services.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.leekleak.trafficlight.MainActivity
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.DataDirection
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.TrafficSnapshot
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.clipAndPad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate

class SpeedNotification(
    serviceScope: CoroutineScope,
    private val context: Context,
    private val notificationId: Int,
    private val networkUsageManager: NetworkUsageManager,
    private val notificationManager: NotificationManager,
    private val connectivityManager: ConnectivityManager,
    private val appPreferenceRepo: AppPreferenceRepo,
    private val trafficSnapshot: TrafficSnapshot,
) : PersistentNotification {
    private val scope = CoroutineScope(serviceScope.coroutineContext + SupervisorJob(serviceScope.coroutineContext[Job]))
    private var job: Job? = null
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notification: Notification
    private var updateCounter = DATA_UPDATE_FREQ
    private var notificationIconHelper = NotificationIconHelper(context)

    private val queryMobile =
        UsageQuery(
            dataType = DataType.Mobile,
            dataDirection = DataDirection.Bidirectional,
        )
    private val queryWifi =
        UsageQuery(
            dataType = DataType.Wifi,
            dataDirection = DataDirection.Bidirectional,
        )

    private var aodMode = false
    private var inBits = false
    private var liveNotification = false
    private var todayUsage = DayUsage()

    init {
        scope.launch {
            appPreferenceRepo.modeAOD.collect { aodMode = it }
        }
        scope.launch {
            appPreferenceRepo.speedBits.collect { inBits = it }
        }
        scope.launch {
            appPreferenceRepo.liveNotification.collect {
                liveNotification = it
            }
        }
        updateBaseNotification()
        notification.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        notificationManager.notify(notificationId, notification)
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
                    delay(100)
                    trafficSnapshot.updateSnapshot()
                }

                if (updateCounter == DATA_UPDATE_FREQ) {
                    updateTodayUsage()
                    updateCounter = 0
                } else {
                    updateCounter++
                }

                updateNotification(trafficSnapshot)
                trafficSnapshot.setCurrentAsLast()
                delay(900)
            }
        }

    }

    override fun cancel() {
        scope.cancel()
        notificationManager.cancel(notificationId)
    }

    override fun startForeground(service: LifecycleService) {
        service.startForeground(
            notificationId,
            notification,
        )
    }

    override fun screenStateChange(on: Boolean) {
        if (on) start()
        else if (!aodMode) job?.cancel()
    }

    override fun getId(): Int = notificationId

    private var lastTitle: String = ""
    private suspend fun updateNotification(trafficSnapshot: TrafficSnapshot) {
        val data = DataSize(trafficSnapshot.totalSpeed).toString(speed = true, inBits = inBits)
        val title = context.getString(R.string.speed, data)

        if (lastTitle == data) return // If the title is the same, so is the icon.
        else lastTitle = data

        val spacing = 18
        val messageShort =
            context.getString(R.string.wi_fi, DataSize(todayUsage.usage2).toString()).clipAndPad(spacing) +
            context.getString(R.string.mobile, DataSize(todayUsage.usage1).toString())

        val speed = data.substringBefore(" ")
        val unit = data.substringAfter(" ")
        updateBaseNotification()
        notification = notificationBuilder
            .apply {
                if (!liveNotification) {
                    setSmallIcon(notificationIconHelper.createIcon(speed, unit))
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
        notification.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        notificationManager.notify(notificationId, notification)
    }

    private suspend fun updateTodayUsage() {
        val date = LocalDate.now()
        val mobile = networkUsageManager.totalDayUsage(queryMobile, date)
        val wifi = networkUsageManager.totalDayUsage(queryWifi, date)
        todayUsage = DayUsage(date, mobile, wifi)
    }

    private fun updateBaseNotification() {
        val networkAvailable = isNetworkAvailable()
        val channel = if (networkAvailable) NOTIFICATION_CHANNEL_ID else NOTIFICATION_CHANNEL_ID_SILENT
        notificationBuilder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name_short))
            .setOngoing(true)
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
        const val NOTIFICATION_CHANNEL_ID = "PersistentNotification"
        const val NOTIFICATION_CHANNEL_ID_SILENT = "PersistentNotification (Silent)"
    }
}