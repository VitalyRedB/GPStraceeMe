package com.githubvitalyredb.gpstraceeme

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Класс отвечает за формирование данных GPS и передачу их в SendService.
 * Он больше не хранит и не отправляет данные сам.
 */
class GpsTrackerManager(
    private val token: String,               // Токен авторизации
    private val userId: String,              // Идентификатор пользователя
    private val onJsonSent: (String) -> Unit // Callback — JSON передаётся в UI
) {

    private val TAG = "GpsTrackerManager"
    private val gson = Gson()

    /**
     * Основной метод — вызывается из TrackerService при получении новой точки.
     */
    fun sendGpsPoint(context: Context, lat: Double, lon: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dateTime = getCurrentDateTime()

                // Формируем объект данных
                val gpsPoint = GpsPoint(
                    token = token,
                    user_id = userId,
                    date = dateTime.first,
                    time = dateTime.second,
                    lat = lat,
                    lon = lon
                )

                // Конвертируем в JSON
                val jsonBody = gson.toJson(gpsPoint)

                // Отправляем JSON в MainActivity для отображения
                onJsonSent(jsonBody)

                // ✅ Передаём JSON-строку (а не объект) в SendService
                SendService.addPendingPoint(context, jsonBody)

                // Запускаем SendService, чтобы он проверил интернет и отправил данные
                val intent = Intent(context, SendService::class.java)
                context.startService(intent)

                Log.i(TAG, "GpsTrackerManager → Точка добавлена в очередь отправки")

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при формировании GPS-точки: ${e.message}", e)
            }
        }
    }

    /**
     * Возвращает текущие дату и время в виде пары (дата, время).
     */
    private fun getCurrentDateTime(): Pair<String, String> {
        return Pair(Utils.getCurrentDateFormatted(), Utils.getCurrentTimeFormatted())
    }
}

