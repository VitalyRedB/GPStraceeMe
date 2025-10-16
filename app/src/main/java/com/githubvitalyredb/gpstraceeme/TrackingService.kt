package com.githubvitalyredb.gpstraceeme

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.*
import java.util.concurrent.TimeUnit

class TrackingService : Service(), LocationHelper.OnLocationReceivedCallback {

    private val TAG = "GPSTrackerService"
    private lateinit var locationHelper: LocationHelper
    private lateinit var gpsTrackerManager: GpsTrackerManager

    private val handler = Handler(Looper.getMainLooper())
    private val MAX_SLEEP_INTERVAL = TimeUnit.HOURS.toMillis(1)

    private var periodicInterval: Long = TimeUnit.MINUTES.toMillis(10)
    private var startHour: Int = 8
    private var endHour: Int = 20

    private var TOKEN = "SECRET123"
    private var USER_ID = "KOD_ID_123"

    private val periodicTask = object : Runnable {
        override fun run() {
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)

            if (currentHour in startHour until endHour) {
                Log.d(TAG, "Working hours. Requesting location...")
                locationHelper.startLocationUpdates(this@TrackingService)
                handler.postDelayed(this, periodicInterval)
            } else {
                val nextStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, startHour)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= now.timeInMillis) add(Calendar.DATE, 1)
                }
                val delay = Math.min(nextStart.timeInMillis - now.timeInMillis, MAX_SLEEP_INTERVAL)
                handler.postDelayed(this, delay)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        locationHelper = LocationHelper(this, null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startHour = intent?.getIntExtra(EXTRA_START_HOUR, 8) ?: 8
        endHour = intent?.getIntExtra(EXTRA_END_HOUR, 20) ?: 20
        val intervalMinutes = intent?.getIntExtra(EXTRA_INTERVAL, 10) ?: 10
        periodicInterval = TimeUnit.MINUTES.toMillis(intervalMinutes.toLong())

        TOKEN = intent?.getStringExtra(EXTRA_TOKEN) ?: TOKEN
        USER_ID = intent?.getStringExtra(EXTRA_USER_ID) ?: USER_ID

        gpsTrackerManager = GpsTrackerManager(TOKEN, USER_ID) { json ->
            val broadcastIntent = Intent(ACTION_UPDATE_MESSAGE).apply {
                putExtra(EXTRA_JSON_MESSAGE, json)
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
        }

        startForeground(NOTIFICATION_ID, createNotification())
        handler.removeCallbacks(periodicTask)
        handler.post(periodicTask)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(periodicTask)
        locationHelper.stopLocationUpdates()
    }

    override fun onBind(intent: Intent?) = null

    override fun onLocationReceived(location: Location) {
        locationHelper.stopLocationUpdates()

        gpsTrackerManager.sendGpsPoint(this, location.latitude, location.longitude)
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "GPS Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("GPStraceeMe")
            .setContentText("Tracking: $startHour:00-$endHour:00 every ${periodicInterval/60000} min")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "gps_tracker_channel"
        const val NOTIFICATION_ID = 101

        const val EXTRA_START_HOUR = "EXTRA_START_HOUR"
        const val EXTRA_END_HOUR = "EXTRA_END_HOUR"
        const val EXTRA_INTERVAL = "EXTRA_INTERVAL"
        const val EXTRA_TOKEN = "EXTRA_TOKEN"
        const val EXTRA_USER_ID = "EXTRA_USER_ID"

        const val ACTION_UPDATE_MESSAGE = "com.githubvitalyredb.gpstraceeme.ACTION_UPDATE_MESSAGE"
        const val EXTRA_JSON_MESSAGE = "EXTRA_JSON_MESSAGE"
    }
}




