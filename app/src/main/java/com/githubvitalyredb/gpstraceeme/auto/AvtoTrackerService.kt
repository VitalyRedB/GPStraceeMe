package com.githubvitalyredb.gpstraceeme.auto

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import com.githubvitalyredb.gpstraceeme.SendService

/**
 * AvtoTrackerService
 *
 * AUTO режим GPS трекера.
 * Работает независимо от ручного TrackerService.
 */

class AvtoTrackerService : Service() {

    companion object {
        private const val TAG = "AvtoTrackerService"
    }

    private lateinit var engine: AutoTrackingEngine

    private lateinit var locationProvider: LocationProvider

    private val filter = LocationFilter()

    private val stateMachine = TrackingStateMachine()

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "AUTO трекер создан")

        locationProvider = LocationProvider(this) { location ->
            onNewLocation(location)
        }

        startForeground(2, createNotification())
        engine = AutoTrackingEngine(this)
        engine.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d(TAG, "AUTO трекер запущен")

        val interval = stateMachine.getUpdateInterval()

        locationProvider.startLocationUpdates(interval)

        return START_STICKY
    }

    override fun onDestroy() {

        Log.d(TAG, "AUTO трекер остановлен")

        locationProvider.stopLocationUpdates()

        super.onDestroy()
        engine.stop()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Обработка новой GPS точки
     */
    private fun onNewLocation(location: Location) {

        Log.d(TAG, "Новая GPS точка")

        if (!filter.shouldUseLocation(location)) {
            return
        }

        if (!stateMachine.processLocation(location)) {
            return
        }

        savePoint(location)

        val newInterval = stateMachine.getUpdateInterval()

        Log.d(TAG, "Новый интервал GPS = $newInterval")
    }

    /**
     * Сохранение точки и отправка через SendService
     */
    private fun savePoint(location: Location) {

        Log.d(TAG, "Сохраняем точку ${location.latitude}, ${location.longitude}")

        val intent = Intent(this, SendService::class.java)

        intent.putExtra("lat", location.latitude)
        intent.putExtra("lon", location.longitude)

        startService(intent)
    }

    /**
     * Упрощённая notification для foreground service
     */
    private fun createNotification(): Notification {

        val builder = Notification.Builder(this)

        builder.setContentTitle("AUTO GPS Tracker")
        builder.setContentText("Трекер работает")
        builder.setSmallIcon(android.R.drawable.ic_menu_mylocation)

        return builder.build()
    }
}
