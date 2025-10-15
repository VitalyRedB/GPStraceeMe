package com.githubvitalyredb.gpstraceeme

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

private val gson = Gson()
private val JSON = "application/json; charset=utf-8".toMediaType()
private val CLIENT = OkHttpClient()

class GpsTrackerManager(
    private val token: String,
    private val userId: String,
    private val onJsonSent: (String) -> Unit // Callback для отправки JSON
) {
    private val url = "https://redburngpscontrol.pythonanywhere.com/api/add_point"

    fun sendGpsPoint(lat: Double, lon: Double) {
        GlobalScope.launch(Dispatchers.IO) {
            val dateTime = getCurrentDateTime()

            val gpsPoint = GpsPoint(
                token = token,
                user_id = userId,
                date = dateTime.first,
                time = dateTime.second,
                lat = lat,
                lon = lon
            )

            val jsonBody = gson.toJson(gpsPoint)
            onJsonSent(jsonBody) // Отправляем JSON в MainActivity

            val body = jsonBody.toRequestBody(JSON)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            try {
                CLIENT.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        println("Failed to post point: ${response.code} ${response.message}")
                    } else {
                        println("GPS point sent successfully. Response: ${response.body?.string()}")
                    }
                }
            } catch (e: IOException) {
                println("Network error: ${e.message}")
            }
        }
    }

    private fun getCurrentDateTime(): Pair<String, String> {
        return Pair(Utils.getCurrentDateFormatted(), Utils.getCurrentTimeFormatted())
    }
}



