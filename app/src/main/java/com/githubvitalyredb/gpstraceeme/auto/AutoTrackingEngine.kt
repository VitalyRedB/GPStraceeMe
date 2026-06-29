package com.githubvitalyredb.gpstraceeme.auto

import android.content.Context
import android.util.Log

class AutoTrackingEngine(private val context: Context) {

    companion object {
        private const val TAG = "AutoEngine"
    }

    private val motionDetector = MotionDetector(context)

    // ✅ ПРАВИЛЬНО: передаем context и callback
    private val locationProvider = LocationProvider(context) { location ->
        Log.d(TAG, "Новая точка: ${location.latitude}, ${location.longitude}")

        // позже:
        // motionFilter.process(location)
        // storage.save(location)
        // sender.send()
    }

    private var isTracking = false

    /**
     * Запуск AUTO режима
     */
    fun start() {
        Log.d(TAG, "AUTO Engine запущен")

        motionDetector.onMotionChanged = { isMoving ->

            if (isMoving) {
                Log.d(TAG, "Движение обнаружено → включаем GPS")
                startTracking()
            } else {
                Log.d(TAG, "Нет движения → выключаем GPS")
                stopTracking()
            }
        }

        motionDetector.start()
    }

    /**
     * Остановка AUTO режима
     */
    fun stop() {
        Log.d(TAG, "AUTO Engine остановлен")

        motionDetector.stop()
        stopTracking()
    }

    /**
     * Включаем GPS
     */
    private fun startTracking() {
        if (isTracking) return

        isTracking = true

        // ✅ ТОЛЬКО интервал!
        locationProvider.startLocationUpdates(5000L)
    }

    /**
     * Выключаем GPS
     */
    private fun stopTracking() {
        if (!isTracking) return

        isTracking = false
        locationProvider.stopLocationUpdates()
    }
}

