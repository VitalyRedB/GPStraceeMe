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

class SendService : Service() {

    companion object {
        private const val TAG = "SendService"
        private const val PREFS_NAME = "send_prefs"
        private const val KEY_TEMP_LIST = "temp_list"
        @Volatile private var isRunning = false

        /** Добавляет JSON-точку в очередь */
        fun addPendingPoint(context: Context, jsonPoint: String) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val gson = Gson()
                val type = object : TypeToken<MutableList<String>>() {}.type
                val list: MutableList<String> =
                    gson.fromJson(prefs.getString(KEY_TEMP_LIST, "[]"), type) ?: mutableListOf()
                list.add(jsonPoint)
                prefs.edit().putString(KEY_TEMP_LIST, gson.toJson(list)).apply()
                Log.d(TAG, "Добавлена точка в очередь — всего: ${list.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при добавлении точки: ${e.message}", e)
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
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка в processPendingPoints: ${e.message}", e)
            } finally {
                isRunning = false
                Log.d(TAG, "isRunning сброшен → сервис завершает работу")
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    /** Основная логика */
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
            Log.d(TAG, "Нет интернета — откладываем (${tempList.size})")
            return
        }

        Log.d(TAG, "Начинаем отправку ${tempList.size} точек")

        val iterator = tempList.iterator()
        var successCount = 0

        while (iterator.hasNext()) {
            val json = iterator.next()
            val success = sendJsonToServer(json)
            if (success) {
                iterator.remove()
                successCount++
                Log.d(TAG, "✅ Точка отправлена, осталось: ${tempList.size}")
            } else {
                Log.w(TAG, "⚠️ Ошибка при отправке — выходим из цикла")
                break
            }
        }

        prefs.edit().putString(KEY_TEMP_LIST, gson.toJson(tempList)).apply()

        if (successCount > 0 && tempList.isEmpty()) {
            playSuccessSound()
        }

        Log.d(TAG, "Отправка завершена. Успешно: $successCount, Осталось: ${tempList.size}")
    }

    /** Отправка JSON на сервер */
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
                    Log.i(TAG, "Отправлено успешно: ${response.code}")
                    true
                } else {
                    Log.e(TAG, "Ошибка ответа: ${response.code} ${response.message}")
                    false
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}")
            false
        }
    }

    /** Проверка интернета */
    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    private fun playSuccessSound() {
        try {
            val mp = MediaPlayer.create(this, R.raw.send_ok)
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (e: Exception) {
            Log.w(TAG, "Ошибка при воспроизведении звука: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}


