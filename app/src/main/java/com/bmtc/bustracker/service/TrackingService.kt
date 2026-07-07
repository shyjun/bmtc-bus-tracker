package com.bmtc.bustracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bmtc.bustracker.MainActivity
import com.bmtc.bustracker.R
import com.bmtc.bustracker.data.repository.BmtcRepository
import com.bmtc.bustracker.data.repository.TrackingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class TrackingService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var repository: BmtcRepository
    private var pollingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = BmtcRepository.getInstance(this)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val busNumber = repository.getSavedBusNumber()
        val vehicleId = repository.getSavedVehicleId()
        val monitoringEnabled = repository.getMonitoringEnabled()

        if (!monitoringEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Start Foreground immediately
        val notification = createForegroundNotification(busNumber)
        startForeground(SERVICE_NOTIFICATION_ID, notification)

        // Start Polling Loop if not already running
        startPolling(busNumber, vehicleId)

        return START_STICKY
    }

    private fun startPolling(busNumber: String, vehicleId: Int) {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            while (true) {
                val result = repository.fetchTrackingUpdate(busNumber, vehicleId)
                
                // After fetching, check freshness
                val state = repository.uiState.value
                val location = state.locationDetails
                
                if (state.trackingStatus == TrackingStatus.OFFLINE) {
                    // Stale tracking
                    if (!repository.isStaleNotificationSent()) {
                        showStaleNotification(busNumber, location?.lastRefreshOn)
                        repository.setStaleNotificationSent(true)
                    }
                } else if (state.trackingStatus == TrackingStatus.ACTIVE) {
                    // Active tracking
                    cancelStaleNotification()
                    repository.setStaleNotificationSent(false)
                }

                // Wait 5 minutes before polling again
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    private fun showStaleNotification(busNumber: String, lastRefreshOn: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val timeOnly = lastRefreshOn?.let {
            val date = repository.parseDate(it)
            if (date != null) {
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                sdf.timeZone = TimeZone.getDefault()
                sdf.format(date)
            } else {
                it
            }
        } ?: "Unknown"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationText = getString(R.string.notification_text_stale_format, busNumber, timeOnly)

        val builder = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(getString(R.string.notification_title_stale))
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(STALE_NOTIFICATION_ID, builder.build())
    }

    private fun cancelStaleNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(STALE_NOTIFICATION_ID)
    }

    private fun createForegroundNotification(busNumber: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = getString(R.string.monitoring_checking_desc)

        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(getString(R.string.notification_title_active, busNumber))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Foreground Service Channel
            val fgsChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(fgsChannel)

            // Alert Alerts Channel
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                getString(R.string.stale_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.stale_channel_desc)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val FOREGROUND_CHANNEL_ID = "bmtc_tracker_service_channel"
        private const val ALERT_CHANNEL_ID = "bmtc_tracker_alerts_channel"
        private const val SERVICE_NOTIFICATION_ID = 1001
        private const val STALE_NOTIFICATION_ID = 1002
        
        // 5 minutes in milliseconds
        private const val POLLING_INTERVAL_MS = 5 * 60 * 1000L
    }
}
