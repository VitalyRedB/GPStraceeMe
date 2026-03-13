package com.githubvitalyredb.gpstraceeme.auto

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*

/**
 * LocationProvider
 *
 * Получает GPS координаты через FusedLocationProvider.
 * Используется в AUTO режиме трекера.
 *
 * Отдает новые координаты через callback.
 */

class LocationProvider(
    context: Context,
    private val callback: (Location) -> Unit
) {

    companion object {
        private const val TAG = "LocationProvider"
    }

    private val fusedClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null

    /**
     * Запуск получения GPS координат
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates(interval: Long) {

        Log.d(TAG, "Запуск GPS обновлений. Интервал = $interval")

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            interval
        ).build()

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(result: LocationResult) {

                val location = result.lastLocation ?: return

                Log.d(TAG, "Получена новая GPS точка")

                callback(location)
            }
        }

        fusedClient.requestLocationUpdates(
            request,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    /**
     * Остановка GPS
     */
    fun stopLocationUpdates() {

        Log.d(TAG, "Остановка GPS")

        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
        }
    }
}
