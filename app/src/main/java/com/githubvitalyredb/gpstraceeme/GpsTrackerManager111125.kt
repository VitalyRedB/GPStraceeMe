package com.githubvitalyredb.gpstraceeme

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * Класс отвечает за отправку GPS-координат на сервер.
 * Если интернет недоступен, точки сохраняются локально и отправляются позже.
 */
class GpsTrackerManager111125(
    private val token: String,                     // Токен для авторизации на сервере
    private val userId: String,                    // ID трекера или пользователя
    private val onJsonSent: (String) -> Unit       // Callback — отправляет JSON в MainActivity для отображения
) {
    private val TAG = "GpsTrackerManager"
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient()
    private val url = "https://redburngpscontrol.pythonanywhere.com/api/add_point"

    /**
     * Основной метод — вызывается для отправки GPS-точки.
     * @param context — используется для проверки сети и сохранения данных локально.
     * @param lat — широта.
     * @param lon — долгота.
     */
    fun sendGpsPoint(context: Context, lat: Double, lon: Double) {
        GlobalScope.launch(Dispatchers.IO) {
            // Получаем дату и время
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

            // Отправляем JSON в активити для отображения на экране
            onJsonSent(jsonBody)

            // Проверяем наличие интернета
            if (isInternetAvailable(context)) {
                // Если интернет есть — пробуем отправить сразу
                if (postPoint(jsonBody)) {
                    // Если успешно — пробуем также отправить накопленные точки
                    sendPendingPoints(context)
                } else {
                    // Если сервер ответил ошибкой — сохраняем точку в офлайн-хранилище
                    savePendingPoint(context, jsonBody)
                }
            } else {
                // Если нет интернета — сохраняем точку локально
                Log.w(TAG, "Нет интернета — сохраняем точку")
                savePendingPoint(context, jsonBody)
            }
        }
    }

    /**
     * Отправляет одну GPS-точку на сервер.
     * @return true, если отправка успешна.
     */
    private fun postPoint(jsonBody: String): Boolean {
        val body = jsonBody.toRequestBody(JSON)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.i(TAG, "GPS point sent successfully: ${response.code}")
                    true
                } else {
                    Log.e(TAG, "Ошибка отправки: ${response.code} ${response.message}")
                    false
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}")
            false
        }
    }

    /**
     * Сохраняет JSON-точку в SharedPreferences при отсутствии сети.
     */
    private fun savePendingPoint(context: Context, jsonString: String) {
        val prefs = context.getSharedPreferences("gps_pending", Context.MODE_PRIVATE)
        val list = prefs.getStringSet("points", mutableSetOf())!!.toMutableSet()
        list.add(jsonString)
        prefs.edit().putStringSet("points", list).apply()
        Log.d(TAG, "Сохранена офлайн-точка (${list.size})")
    }

    /**
     * После восстановления соединения — пытается отправить все сохранённые точки.
     */
    private fun sendPendingPoints(context: Context) {
        val prefs = context.getSharedPreferences("gps_pending", Context.MODE_PRIVATE)
        val list = prefs.getStringSet("points", emptySet())!!.toMutableSet()
        if (list.isEmpty()) return

        Log.i(TAG, "Пробуем отправить ${list.size} накопленных точек")

        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val json = iterator.next()
            if (postPoint(json)) {
                iterator.remove() // если успешно — удаляем точку из списка
            } else {
                Log.e(TAG, "Ошибка при повторной отправке, выходим")
                break
            }
        }

        // Обновляем SharedPreferences — сохраняем оставшиеся точки
        prefs.edit().putStringSet("points", list).apply()
    }

    /**
     * Проверка наличия активного интернет-соединения (Wi-Fi или мобильный интернет).
     */
    private fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Возвращает текущие дату и время в виде пары (дата, время).
     */
    private fun getCurrentDateTime(): Pair<String, String> {
        return Pair(Utils.getCurrentDateFormatted(), Utils.getCurrentTimeFormatted())
    }
}




