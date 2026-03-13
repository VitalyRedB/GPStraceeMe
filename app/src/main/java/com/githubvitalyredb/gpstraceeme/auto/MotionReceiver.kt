package com.githubvitalyredb.gpstraceeme.auto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

/**
 * MotionReceiver
 *
 * Получает события движения от Android.
 */

class MotionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MotionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {

        if (!ActivityTransitionResult.hasResult(intent)) {
            return
        }

        val result = ActivityTransitionResult.extractResult(intent) ?: return

        for (event in result.transitionEvents) {

            val activity = event.activityType

            when (activity) {

                DetectedActivity.STILL -> {
                    Log.d(TAG, "Устройство стоит")
                }

                DetectedActivity.WALKING -> {
                    Log.d(TAG, "Пользователь идет")
                }

                DetectedActivity.IN_VEHICLE -> {
                    Log.d(TAG, "Пользователь едет")
                }
            }
        }
    }
}
