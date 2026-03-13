package com.githubvitalyredb.gpstraceeme.auto

import android.content.Context
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import android.app.PendingIntent
import android.content.Intent

/**
 * MotionDetector
 *
 * Определяет движение устройства:
 * - стоит
 * - идет
 * - едет
 *
 * Используется для экономии батареи:
 * GPS включается только когда устройство движется.
 */

class MotionDetector(private val context: Context) {

    companion object {
        private const val TAG = "MotionDetector"
    }

    private val activityClient: ActivityRecognitionClient =
        ActivityRecognition.getClient(context)

    /**
     * Callback который сообщает сервису
     * что устройство начало или закончило движение
     */
    var onMotionChanged: ((Boolean) -> Unit)? = null

    /**
     * PendingIntent для получения событий движения
     */
    private val pendingIntent: PendingIntent by lazy {

        val intent = Intent(context, MotionReceiver::class.java)

        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Запуск детектора движения
     */
    fun start() {

        Log.d(TAG, "Запуск MotionDetector")

        val transitions = listOf(

            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),

            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),

            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )

        val request = ActivityTransitionRequest(transitions)

        activityClient.requestActivityTransitionUpdates(
            request,
            pendingIntent
        )
    }

    /**
     * Остановка детектора движения
     */
    fun stop() {

        Log.d(TAG, "Остановка MotionDetector")

        activityClient.removeActivityTransitionUpdates(pendingIntent)
    }
}
