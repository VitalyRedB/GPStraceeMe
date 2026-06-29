package com.githubvitalyredb.gpstraceeme.auto

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.*

class MotionDetector(private val context: Context) {

    companion object {
        private const val TAG = "MotionDetector"
    }

    // 👉 callback наружу (в Engine)
    var onMotionChanged: ((Boolean) -> Unit)? = null

    private val activityClient = ActivityRecognition.getClient(context)

    private val intent by lazy {
        Intent(context, MotionReceiver::class.java)
    }

    private val pendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Запуск отслеживания движения
     */
    fun start() {
        Log.d(TAG, "Старт отслеживания движения")

        activityClient.requestActivityUpdates(
            5000, // каждые 5 сек проверка
            pendingIntent
        )
    }

    /**
     * Остановка
     */
    fun stop() {
        Log.d(TAG, "Остановка отслеживания движения")

        activityClient.removeActivityUpdates(pendingIntent)
    }

    /**
     * Вызов из Receiver (важно!)
     */
    fun handleActivityResult(result: ActivityRecognitionResult) {

        val activity = result.mostProbableActivity
        val type = activity.type

        Log.d(TAG, "Activity detected: $type")

        val isMoving = when (type) {
            DetectedActivity.IN_VEHICLE -> true
            DetectedActivity.ON_BICYCLE -> true
            DetectedActivity.WALKING -> true
            else -> false
        }

        onMotionChanged?.invoke(isMoving)
    }
}

