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

        if (!ActivityTransitionResult.hasResult(intent)) return

        val result = ActivityTransitionResult.extractResult(intent) ?: return

        for (event in result.transitionEvents) {

            val activity = event.activityType

            val isMoving = when (activity) {
                DetectedActivity.STILL -> false
                DetectedActivity.WALKING -> true
                DetectedActivity.IN_VEHICLE -> true
                else -> false
            }

            Log.d(TAG, "Motion: $activity → moving=$isMoving")

            // отправляем broadcast в Engine
            val motionIntent = Intent("MOTION_UPDATE")
            motionIntent.putExtra("isMoving", isMoving)
            context.sendBroadcast(motionIntent)
        }
    }
}
