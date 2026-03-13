package com.githubvitalyredb.gpstraceeme.auto

import android.location.Location
import android.util.Log

/**
 * State Machine — ядро AUTO трекера.
 *
 * Определяет текущее состояние движения устройства
 * и решает как часто нужно снимать координаты.
 *
 * Это позволяет:
 * - экономить батарею
 * - уменьшать количество GPS точек
 * - уменьшать сетевой трафик
 */

class TrackingStateMachine {

    companion object {
        private const val TAG = "TrackingStateMachine"

        // Минимальное расстояние между точками (метры)
        private const val MIN_DISTANCE = 20f

        // Скорость, при которой считаем что устройство движется (км/ч)
        private const val MOVING_SPEED = 5f

        // Скорость, при которой считаем что это автомобиль
        private const val VEHICLE_SPEED = 25f
    }

    /**
     * Возможные состояния трекера
     */
    enum class TrackingState {
        IDLE,       // устройство стоит
        MOVING,     // человек идет
        VEHICLE     // едет на машине
    }

    private var currentState = TrackingState.IDLE
    private var lastLocation: Location? = null

    /**
     * Основной метод обработки новой GPS точки
     */
    fun processLocation(newLocation: Location): Boolean {

        val previousLocation = lastLocation
        lastLocation = newLocation

        if (previousLocation == null) {
            Log.d(TAG, "Первая точка получена")
            return true
        }

        val distance = previousLocation.distanceTo(newLocation)

        val speedKmh = newLocation.speed * 3.6f

        Log.d(TAG, "Distance=$distance m Speed=$speedKmh kmh")

        updateState(speedKmh)

        /**
         * Фильтр расстояния
         * Если устройство почти не сдвинулось — точку игнорируем
         */
        if (distance < MIN_DISTANCE) {
            Log.d(TAG, "Точка отброшена — слишком маленькое расстояние")
            return false
        }

        return true
    }

    /**
     * Обновление состояния движения
     */
    private fun updateState(speed: Float) {

        val newState = when {
            speed >= VEHICLE_SPEED -> TrackingState.VEHICLE
            speed >= MOVING_SPEED -> TrackingState.MOVING
            else -> TrackingState.IDLE
        }

        if (newState != currentState) {
            Log.d(TAG, "Смена состояния: $currentState -> $newState")
            currentState = newState
        }
    }

    /**
     * Возвращает интервал обновления GPS
     * в зависимости от текущего состояния
     */
    fun getUpdateInterval(): Long {

        return when (currentState) {

            TrackingState.IDLE -> {
                // Стоим — обновление раз в 5 минут
                5 * 60 * 1000L
            }

            TrackingState.MOVING -> {
                // Идем пешком — раз в 60 секунд
                60 * 1000L
            }

            TrackingState.VEHICLE -> {
                // Едем на машине — каждые 10 секунд
                10 * 1000L
            }
        }
    }

    /**
     * Возвращает текущее состояние
     */
    fun getCurrentState(): TrackingState {
        return currentState
    }
}
