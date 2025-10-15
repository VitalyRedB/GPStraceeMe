package com.githubvitalyredb.gpstraceeme

data class StepData(val initialSteps: Float, val runningSteps: Float, val startTime: Long)

// ADD Class : Модель для отправки данных на сервер
data class GpsPoint(
    val token: String,
    val user_id: String,
    val date: String,
    val time: String,
    val lat: Double,
    val lon: Double
)