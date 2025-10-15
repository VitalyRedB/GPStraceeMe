package com.githubvitalyredb.gpstraceeme

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.location.Granularity

class LocationHelper(private val context: Context, private val activity: Activity?) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    // Интерфейс для передачи координат обратно в MainActivity
    // Я переименовал его, чтобы избежать конфликта с LocationCallback из Google Play Services
    interface OnLocationReceivedCallback {
        fun onLocationReceived(location: Location)
    }

    fun startLocationUpdates(callback: OnLocationReceivedCallback) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (activity != null) { // Безопасная проверка!

                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
                )
                Toast.makeText(
                    context,
                    "Предоставьте разрешение на местоположение",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Логируем, что нет разрешения (мы в сервисе)
                Log.e("LocationHelper", "Нет разрешения. Невозможно запросить из Service.")
            }
            return
        }
        // *************************************************************
        // ИЗМЕНЕНИЕ: Настройка LocationRequest для 10 минут
        // PRIORITY_HIGH_ACCURACY — для GPS-координат
        // 600000 мс = 10 минут
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setGranularity(Granularity.GRANULARITY_FINE)
            // minUpdateIntervalMillis можно оставить меньше (например, 60000 мс = 1 минута),
            // чтобы система не переключалась между 10-минутными интервалами, но
            // для строгого 10-минутного обновления 600000 будет работать лучше.
            .setMinUpdateIntervalMillis(600000)
            .build()
        // *************************************************************

        // Теперь мы используем LocationCallback из Google Play Services
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    // И передаем данные в наш собственный колбэк
                    callback.onLocationReceived(it)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 1
    }
}