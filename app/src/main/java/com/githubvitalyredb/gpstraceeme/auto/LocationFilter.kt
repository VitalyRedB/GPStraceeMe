package com.githubvitalyredb.gpstraceeme.auto

import android.location.Location
import android.util.Log

/**
 * LocationFilter
 *
 * Этот класс фильтрует "плохие" GPS точки:
 *
 * 1. Слишком плохая точность GPS
 * 2. Нереальная скорость (GPS глюк)
 * 3. Слишком маленькое расстояние (GPS шум)
 *
 * Это делает трек:
 * - аккуратным
 * - экономит батарею
 * - уменьшает трафик
 */

class LocationFilter {

    companion object {
        private const val TAG = "LocationFilter"

        // минимальное расстояние между точками
        private const val MIN_DISTANCE_METERS = 15f

        // максимальная допустимая скорость (км/ч)
        private const val MAX_SPEED_KMH = 180f

        // максимальная допустимая погрешность GPS
        private const val MAX_ACCURACY_METERS = 50f
    }

    private var lastLocation: Location? = null

    /**
     * Проверяет можно ли сохранять точку
     */
    fun shouldUseLocation(newLocation: Location): Boolean {

        /**
         * 1️⃣ Проверяем точность GPS
         */
        if (newLocation.accuracy > MAX_ACCURACY_METERS) {

            Log.d(TAG, "Точка отброшена: плохая точность ${newLocation.accuracy}")
            return false
        }

        val previous = lastLocation

        if (previous == null) {
            lastLocation = newLocation
            Log.d(TAG, "Первая точка принята")
            return true
        }

        /**
         * 2️⃣ Проверяем расстояние
         */
        val distance = previous.distanceTo(newLocation)

        if (distance < MIN_DISTANCE_METERS) {

            Log.d(TAG, "Точка отброшена: слишком близко ($distance м)")
            return false
        }

        /**
         * 3️⃣ Проверяем скорость
         */
        val speedKmh = newLocation.speed * 3.6f

        if (speedKmh > MAX_SPEED_KMH) {

            Log.d(TAG, "Точка отброшена: нереальная скорость $speedKmh км/ч")
            return false
        }

        lastLocation = newLocation

        Log.d(TAG, "Точка принята: расстояние $distance м скорость $speedKmh")

        return true
    }
}
