package com.githubvitalyredb.gpstraceeme

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import java.util.concurrent.TimeUnit

class TrackerService : Service(), LocationHelper.OnLocationReceivedCallback {

    private val TAG = "TrackerService"

    private lateinit var locationHelper: LocationHelper
    private lateinit var gpsTrackerManager: GpsTrackerManager
    private val handler = Handler(Looper.getMainLooper())

    private var periodicInterval: Long = TimeUnit.MINUTES.toMillis(10)
    private val MAX_SLEEP_INTERVAL = TimeUnit.HOURS.toMillis(1)
    private var startHour = 8
    private var endHour = 20
    private var TOKEN = "SECRET123"
    private var TRACKER_NAME = "KOD_ID_123"
    private var backgroundMessagesEnabled = true
    private var daysMap: MutableMap<String, Int> = mutableMapOf()
    private var periodicRunning = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate: запуск Foreground-сервиса")
        try {
            locationHelper = LocationHelper(this, null)
            loadSettings()

            gpsTrackerManager = GpsTrackerManager(TOKEN, TRACKER_NAME) { json ->
                val intent = Intent(ACTION_UPDATE_MESSAGE).apply {
                    putExtra(EXTRA_JSON_MESSAGE, json)
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            }

            startForeground(NOTIFICATION_ID, createNotification())
            sendServiceStatus(true)

            if (!periodicRunning) {
                periodicRunning = true
                handler.post(periodicTask)
                Log.d(TAG, "Периодическая задача запущена в onCreate.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка запуска сервиса: ${e.message}", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Intent получен. startId=$startId")

        // 🔹 Обновляем настройки из MainActivity или из SharedPreferences
        intent?.let {
            startHour = it.getIntExtra(EXTRA_START_HOUR, startHour)
            endHour = it.getIntExtra(EXTRA_END_HOUR, endHour)
            val intervalMinutes = it.getIntExtra(
                EXTRA_INTERVAL,
                (periodicInterval / TimeUnit.MINUTES.toMillis(1)).toInt()
            )
            periodicInterval = TimeUnit.MINUTES.toMillis(intervalMinutes.toLong())
            TOKEN = it.getStringExtra(EXTRA_TOKEN) ?: TOKEN
            TRACKER_NAME = it.getStringExtra(EXTRA_TRACKER_NAME) ?: TRACKER_NAME
            backgroundMessagesEnabled =
                it.getBooleanExtra(EXTRA_BACKGROUND_MESSAGES, backgroundMessagesEnabled)

            Log.d(TAG, "Параметры из MainActivity: Start=$startHour, End=$endHour, Interval=$intervalMinutes, Token=${TOKEN.take(4)}..., UserTrackerName=$TRACKER_NAME")
        }

        // 🔹 Обновляем gpsTrackerManager с актуальными TOKEN и USER_ID
        gpsTrackerManager = GpsTrackerManager(TOKEN, TRACKER_NAME) { json ->
            val broadcastIntent = Intent(ACTION_UPDATE_MESSAGE).apply {
                putExtra(EXTRA_JSON_MESSAGE, json)
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
        }

        // 🔹 Обновляем уведомление
        (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
            ?.notify(NOTIFICATION_ID, createNotification())

        // 🔹 Экстренное снятие координат по кнопке СТАРТ
        if (intent?.action == ACTION_REQUEST_IMMEDIATE_LOCATION) {
            Log.d(TAG, "Экстренное снятие координат по кнопке СТАРТ")

            // Снимаем координаты
            locationHelper.startLocationUpdates(this)

            // Сброс старого periodicTask и перезапуск с новым интервалом
            handler.removeCallbacks(periodicTask)
            periodicRunning = false
            handler.postDelayed(periodicTask, periodicInterval)
            periodicRunning = true

            try {
                val mp = MediaPlayer.create(this, R.raw.click_sound)
                mp.setOnCompletionListener { it.release() }
                mp.start()
            } catch (e: Exception) {
                Log.w(TAG, "Ошибка при воспроизведении звука: ${e.message}")
            }

            return START_STICKY
        }

        // 🔹 Стандартный запуск periodicTask (если ещё не запущен)
        if (!periodicRunning) {
            Log.d(TAG, "Запуск periodicTask")
            handler.removeCallbacks(periodicTask) // на всякий случай
            handler.post(periodicTask)
            periodicRunning = true
        } else {
            Log.d(TAG, "periodicTask уже запущен, просто обновлены настройки")
        }

        return START_STICKY
    }


    private val periodicTask = object : Runnable {
        override fun run() {
            try {
                loadSettings()
                val now = Calendar.getInstance()
                val hour = now.get(Calendar.HOUR_OF_DAY)
                val dayName = when (now.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "Mon"
                    Calendar.TUESDAY -> "Tue"
                    Calendar.WEDNESDAY -> "Wed"
                    Calendar.THURSDAY -> "Thu"
                    Calendar.FRIDAY -> "Fri"
                    Calendar.SATURDAY -> "Sat"
                    Calendar.SUNDAY -> "Sun"
                    else -> "Unknown"
                }

                val isDayActive = daysMap[dayName] == 1
                val isTimeActive = hour in startHour until endHour

                if (isDayActive && isTimeActive) {
                    Log.i(TAG, "Активно: $dayName $hour:00 → запрос координат.")
                    locationHelper.startLocationUpdates(this@TrackerService)
                } else {
                    Log.d(TAG, "Неактивно: $dayName $hour:00 → следующая проверка через ${periodicInterval / 60000} мин.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка в periodicTask: ${e.message}", e)
            } finally {
                handler.removeCallbacks(this)
                handler.postDelayed(this, periodicInterval.coerceAtMost(MAX_SLEEP_INTERVAL))
            }
        }
    }

    override fun onLocationReceived(location: Location) {
        locationHelper.stopLocationUpdates()
        Log.i(TAG, "Координаты Х/У: ${location.latitude}, ${location.longitude}")

        gpsTrackerManager.sendGpsPoint(this, location.latitude, location.longitude)
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_MIN
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "GPStraceeMe Tracking",
                importance
            ).apply {
                description = "Фоновое отслеживание координат"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentTitle("GPStraceeMe")
            .setContentText(
                if (backgroundMessagesEnabled)
                    "Tracking: $startHour:00-$endHour:00 every ${periodicInterval / 60000} min"
                else
                    "Tracking active (hidden mode)"
            )
            .build()
    }

    private fun sendServiceStatus(isRunning: Boolean) {
        val intent = Intent("SERVICE_STATUS_UPDATE").apply {
            putExtra("isRunning", isRunning)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        periodicRunning = false
        handler.removeCallbacks(periodicTask)
        locationHelper.stopLocationUpdates()
        sendServiceStatus(false)
        Log.d(TAG, "Service onDestroy: трекер остановлен.")
        super.onDestroy()
    }

    /** Чтение настроек из SharedPreferences (используем AppPrefs как в MainActivity) */
    private fun loadSettings() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        // Если в prefs раньше были строки HH:MM, пытаемся корректно распарсить
        startHour = prefs.getString("START_HOUR", "08:00")?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 8
        endHour = prefs.getString("END_HOUR", "20:00")?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 20

        // Интервал храним в виде "00:10" или в миллисекундах — совместим оба варианта
        val intervalStr = prefs.getString("INTERVAL", null)
        periodicInterval = if (intervalStr != null && intervalStr.contains(":")) {
            TimeUnit.MINUTES.toMillis(intervalStr.split(":").getOrNull(1)?.toLongOrNull() ?: 10)
        } else {
            prefs.getLong("interval", periodicInterval)
        }

        backgroundMessagesEnabled = prefs.getBoolean("background_messages_enabled", true)
        TOKEN = prefs.getString("TOKEN", TOKEN) ?: TOKEN
        TRACKER_NAME = prefs.getString("TRACKER_NAME", TRACKER_NAME) ?: TRACKER_NAME

        val json = prefs.getString("daysMap", null)
        daysMap = if (json != null) {
            val type = object : TypeToken<MutableMap<String, Int>>() {}.type
            Gson().fromJson(json, type)
        } else {
            mutableMapOf("Mon" to 1, "Tue" to 1, "Wed" to 1, "Thu" to 1, "Fri" to 1, "Sat" to 0, "Sun" to 0)
        }

        Log.d(TAG, "SharedPreferences: Загружены настройки: $startHour-$endHour, интервал ${periodicInterval / 60000} мин.")
    }


    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "gps_tracker_channel"
        const val NOTIFICATION_ID = 101

        const val ACTION_UPDATE_MESSAGE = "com.githubvitalyredb.gpstraceeme.ACTION_UPDATE_MESSAGE"
        const val EXTRA_JSON_MESSAGE = "EXTRA_JSON_MESSAGE"

        const val EXTRA_START_HOUR = "EXTRA_START_HOUR"
        const val EXTRA_END_HOUR = "EXTRA_END_HOUR"
        const val EXTRA_INTERVAL = "EXTRA_INTERVAL"
        const val EXTRA_TOKEN = "EXTRA_TOKEN"
        const val EXTRA_TRACKER_NAME = "EXTRA_TRACKER_NAME"
        const val EXTRA_BACKGROUND_MESSAGES = "EXTRA_BACKGROUND_MESSAGES"

        const val ACTION_REQUEST_IMMEDIATE_LOCATION = "com.githubvitalyredb.gpstraceeme.ACTION_REQUEST_IMMEDIATE_LOCATION"
    }
}








