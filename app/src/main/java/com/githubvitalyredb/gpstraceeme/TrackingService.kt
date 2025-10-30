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

class TrackerService : Service() , LocationHelper.OnLocationReceivedCallback {

    private val TAG = "TrackerService"

    private lateinit var locationHelper: LocationHelper
    private lateinit var gpsTrackerManager: GpsTrackerManager
    private val handler = Handler(Looper.getMainLooper())

    // 🔧 Рабочие параметры, загружаемые из SharedPreferences
    private var periodicInterval: Long = TimeUnit.MINUTES.toMillis(10)
    private val MAX_SLEEP_INTERVAL = TimeUnit.HOURS.toMillis(1)
    private var startHour = 8
    private var endHour = 20
    private var TOKEN = "SECRET123"
    private var USER_ID = "KOD_ID_123"
    private var backgroundMessagesEnabled = true
    private var daysMap: MutableMap<String, Int> = mutableMapOf()

    override fun onCreate() {
        // 🚨 КРИТИЧЕСКИЙ ЛОГ: Теперь это первая команда в теле метода!
        super.onCreate()
        Log.d(TAG, "Service onCreate запущен. Первая строчка!")

        try {
            // 🔹 Инициализация LocationHelper
            locationHelper = LocationHelper(this, null)

            // Читаем SharedPreferences для daysMap и fallback-параметров
            loadSettings()

            // 🔹 Инициализация GpsTrackerManager
            gpsTrackerManager = GpsTrackerManager(TOKEN, USER_ID) { json ->
                val intent = Intent(ACTION_UPDATE_MESSAGE).apply {
                    putExtra(EXTRA_JSON_MESSAGE, json)
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            }

            // 🔹 Запуск Foreground-сервиса
            // NOTE: Уведомление создается здесь, но обновляется в onStartCommand
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d(TAG, "Service запущен в Foreground mode.")

            // 🔹 Старт периодической проверки
            handler.post(periodicTask)
        } catch (e: Exception) {
            // 🚨 ЛОГ: Если что-то пошло не так, это будет записано
            Log.e(TAG, "КРИТИЧЕСКАЯ ОШИБКА в onCreate сервиса, причина краша: ${e.message}", e)
            throw e
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Intent получен. startId=$startId")

        // 🔹 Получение параметров из MainActivity, которые имеют приоритет над SharedPreferences
        intent?.let {
            startHour = it.getIntExtra(EXTRA_START_HOUR, startHour)
            endHour = it.getIntExtra(EXTRA_END_HOUR, endHour)
            // Конвертируем минуты обратно в миллисекунды для periodicInterval
            val intervalMinutes = it.getIntExtra(EXTRA_INTERVAL, (periodicInterval / TimeUnit.MINUTES.toMillis(1)).toInt())
            periodicInterval = TimeUnit.MINUTES.toMillis(intervalMinutes.toLong())
            TOKEN = it.getStringExtra(EXTRA_TOKEN) ?: TOKEN
            USER_ID = it.getStringExtra(EXTRA_USER_ID) ?: USER_ID
            backgroundMessagesEnabled = it.getBooleanExtra(EXTRA_BACKGROUND_MESSAGES, backgroundMessagesEnabled)

            // 🔹 Лог о полученных параметрах. Скрываем часть токена.
            Log.d(TAG, "Параметры из Intent: Start=$startHour, End=$endHour, Interval=$intervalMinutes min, Token=${TOKEN.take(4)}..., UserID=$USER_ID")
        }

        // --- ИСПРАВЛЕНИЕ КРАША ЗДЕСЬ ---
        // 🔹 Обновляем уведомление (особенно для backgroundMessagesEnabled)
        // Используем безопасный каст 'as?' с null-safe вызовом.
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (notificationManager == null) {
            Log.e(TAG, "Ошибка: Не удалось получить NotificationManager для обновления уведомления.")
        } else {
            notificationManager.notify(NOTIFICATION_ID, createNotification())
            Log.d(TAG, "Уведомление обновлено в onStartCommand.")
        }
        // -----------------------------

        return START_STICKY
    }

    /**
     * Загружаем настройки из SharedPreferences
     */
    private fun loadSettings() {
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        startHour = prefs.getInt("startHour", 8)
        endHour = prefs.getInt("endHour", 20)
        periodicInterval = prefs.getLong("interval", TimeUnit.MINUTES.toMillis(10))
        backgroundMessagesEnabled = prefs.getBoolean("background_messages_enabled", true)
        TOKEN = prefs.getString("TOKEN", TOKEN) ?: TOKEN
        USER_ID = prefs.getString("USER_ID", USER_ID) ?: USER_ID

        val json = prefs.getString("daysMap", null)
        daysMap = if (json != null) {
            val type = object : TypeToken<MutableMap<String, Int>>() {}.type
            Gson().fromJson(json, type)
        } else {
            mutableMapOf("Mon" to 1, "Tue" to 1, "Wed" to 1, "Thu" to 1, "Fri" to 1, "Sat" to 0, "Sun" to 0)
        }

        Log.d(TAG, "SharedPreferences: Загружены базовые настройки (DaysMap).")
    }

    /**
     * Периодическая задача
     */
    private val periodicTask = object : Runnable {
        override fun run() {
            try {
                loadSettings() // обновляем настройки каждый цикл

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

                // Рассчитываем следующую задержку
                val nextDelay = periodicInterval.coerceAtMost(MAX_SLEEP_INTERVAL)
                val intervalMinutes = nextDelay / 60000

                if (isDayActive && isTimeActive) {
                    Log.i(TAG, "Активно: $dayName, $hour:00. Интервал $intervalMinutes мин. Запрос координат.")
                    // 🔹 Запуск получения координат с колбэком
                    locationHelper.startLocationUpdates(this@TrackerService)
                } else {
                    Log.d(TAG, "Неактивно: $dayName, $hour:00. Пропуск цикла. Следующий цикл через $intervalMinutes мин.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка в periodicTask: ${e.message}", e)
            } finally {
                // Планируем следующий запуск с учетом ограничения MAX_SLEEP_INTERVAL
                handler.postDelayed(this, periodicInterval.coerceAtMost(MAX_SLEEP_INTERVAL))
            }
        }
    }

    /**
     * Реализация OnLocationReceivedCallback
     */
    override fun onLocationReceived(location: Location) {
        locationHelper.stopLocationUpdates()
        // 🔹 ЛОГ: Получение координат
        Log.i(TAG, "КООРДИНАТЫ ПОЛУЧЕНЫ: Lat=${location.latitude}, Lon=${location.longitude}. Отправка на сервер.")

        // 🔹 Проигрываем звук при получении координат
        val mediaPlayer = MediaPlayer.create(this, R.raw.data_sound)
        mediaPlayer.setOnCompletionListener { it.release() }
        mediaPlayer.start()

        // 🔹 Отправляем координаты на сервер и MainActivity
        gpsTrackerManager.sendGpsPoint(this, location.latitude, location.longitude)
    }

    /**
     * Создание уведомления Foreground-сервиса (тихое)
     */
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

    override fun onDestroy() {
        handler.removeCallbacks(periodicTask)
        locationHelper.stopLocationUpdates()
        Log.d(TAG, "Service onDestroy: Остановлено и удалено.")
        super.onDestroy()
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
        const val EXTRA_USER_ID = "EXTRA_USER_ID"
        const val EXTRA_BACKGROUND_MESSAGES = "EXTRA_BACKGROUND_MESSAGES"
    }
}



