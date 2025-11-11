package com.githubvitalyredb.gpstraceeme

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * SendService — отвечает за повторную отправку сохранённых JSON-точек (temp list).
 * Формат точек: строка JSON, совместимая с тем, что отправляет приложение:
 * {"date":"2025-11-11","lat":37.4219983,"lon":-122.084,"time":"08:27:52","token":"SECRET123","user_id":"RedMi5_emu3"}
 */
class SendService : Service() {

    companion object {
        private const val TAG = "SendService"
        private const val PREFS_NAME = "send_prefs"
        private const val KEY_TEMP_LIST = "temp_list"
        @Volatile private var isRunning = false

        /**
         * Добавляет JSON-строку точки в локальную очередь (SharedPreferences).
         * Вызывается из места, где формируется JSON (например, GpsTrackerManager).
         */
        fun addPendingPoint(context: Context, jsonPoint: String) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val gson = Gson()
                val type = object : TypeToken<MutableList<String>>() {}.type
                val currentList: MutableList<String> =
                    gson.fromJson(prefs.getString(KEY_TEMP_LIST, "[]"), type) ?: mutableListOf()
                currentList.add(jsonPoint)
                prefs.edit().putString(KEY_TEMP_LIST, gson.toJson(currentList)).apply()
                Log.d(TAG, "Добавлена точка в очередь — всего: ${currentList.size}")
            } catch (e: Exception) {
                Log.e(TAG, "addPendingPoint error: ${e.message}", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning) {
            Log.d(TAG, "Уже выполняется — пропуск запуска")
            return START_NOT_STICKY
        }

        isRunning = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                processPendingPoints()
            } finally {
                isRunning = false
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    /**
     * Основная логика: читаем очередь (список JSON-строк),
     * если нет интернета — выходим, если есть — отправляем по очереди.
     */
    private fun processPendingPoints() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val type = object : TypeToken<MutableList<String>>() {}.type
        val tempList: MutableList<String> =
            gson.fromJson(prefs.getString(KEY_TEMP_LIST, "[]"), type) ?: mutableListOf()

        if (tempList.isEmpty()) {
            Log.d(TAG, "Очередь пуста — ничего не отправляем")
            return
        }

        if (!isInternetAvailable()) {
            Log.d(TAG, "Нет интернета — откладываем отправку (${tempList.size})")
            return
        }

        Log.d(TAG, "Попытка отправки ${tempList.size} точек")

        val iterator = tempList.iterator()
        var successCount = 0

        while (iterator.hasNext()) {
            val jsonPoint = iterator.next()

            val success = sendJsonToServer(jsonPoint)
            if (success) {
                iterator.remove()
                successCount++
                Log.d(TAG, "Точка отправлена успешно, осталось: ${tempList.size}")
            } else {
                Log.e(TAG, "Ошибка при отправке точки — прерываем цикл")
                break
            }
        }

        // Сохраняем оставшиеся точки обратно в prefs
        prefs.edit().putString(KEY_TEMP_LIST, gson.toJson(tempList)).apply()

        // Если успешно отправили хотя бы одну и очередь пуста — проигрываем звук
        if (successCount > 0 && tempList.isEmpty()) {
            playSuccessSound()
        }

        Log.d(TAG, "processPendingPoints завершен. Успешно: $successCount, Осталось: ${tempList.size}")
    }

    /**
     * Отправляет JSON напрямую на сервер. Возвращает true при успешном ответе (2xx).
     * Используем jsonString как тело запроса.
     */
    private fun sendJsonToServer(jsonString: String): Boolean {
        val client = OkHttpClient()
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        val body = jsonString.toRequestBody(JSON_MEDIA)

        val request = Request.Builder()
            .url("https://redburngpscontrol.pythonanywhere.com/api/add_point")
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.i(TAG, "Точка успешно отправлена: ${response.code}")
                    true
                } else {
                    Log.e(TAG, "Ошибка сервера: ${response.code} ${response.message}")
                    false
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error при отправке: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error при отправке: ${e.message}")
            false
        }
    }

    /** Проверяем наличие интернета (Wi-Fi или мобильный) */
    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    private fun playSuccessSound() {
        try {
            val mp = MediaPlayer.create(this, R.raw.send_ok)
            mp.setOnCompletionListener { it.release() }
            mp.start()
            Log.d(TAG, "✅ Все накопленные данные отправлены — звук проигран")
        } catch (e: Exception) {
            Log.w(TAG, "Ошибка при воспроизведении звука: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

