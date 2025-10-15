package com.githubvitalyredb.gpstraceeme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object Utils {
    // ... (существующая функция formatDuration) ...

    fun formatDuration(millis: Long): String {
        // ...
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    // --- НОВЫЕ ФУНКЦИИ ---

    // Формат времени для сервера (HH:mm:ss)
    fun getCurrentTimeFormatted(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    // Формат даты для сервера (YYYY-MM-DD)
    fun getCurrentDateFormatted(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}