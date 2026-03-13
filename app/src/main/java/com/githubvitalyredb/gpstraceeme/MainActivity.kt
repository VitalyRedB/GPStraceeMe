package com.githubvitalyredb.gpstraceeme

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private val TAG = "MAIN_LOG"

    private lateinit var daysManager: DaysManager
    private lateinit var daysContainer: LinearLayout
    private lateinit var autoOverlay: LinearLayout


    companion object {
        private const val PREFS_NAME = "AppPrefs"
        private const val PERMISSION_REQUEST_CODE = 1001

        var TEST_START_HOUR = 8
        var TEST_END_HOUR = 20
        var TEST_INTERVAL_MINUTES = 10
        var TOKEN = "SECRET123"
        var TRACKER_NAME = "Unknown"

    }

    private lateinit var prefs: SharedPreferences
    private lateinit var textStartHour: TextView
    private lateinit var textEndHour: TextView
    private lateinit var textInterval: TextView
    private lateinit var textToken: TextView
    private lateinit var textTrackerName: TextView
    private lateinit var lastMessageTextView: TextView
    private lateinit var startButton: Button
    private lateinit var settingsButton: Button

    private val jsonReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra(TrackerService.EXTRA_JSON_MESSAGE) ?: "---"
            lastMessageTextView.text = "Last server message: $message"
            lastMessageTextView.setTextColor(android.graphics.Color.YELLOW)
            Log.d(TAG, "Получено сообщение: $message")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate")

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // maska avtoMode
        autoOverlay = findViewById(R.id.auto_mode_overlay)

        daysContainer = findViewById(R.id.daysContainer)
        daysManager = DaysManager(this)
        daysManager.drawDays(daysContainer)

        textStartHour = findViewById(R.id.text_start_hour)
        textEndHour = findViewById(R.id.text_end_hour)
        textInterval = findViewById(R.id.text_interval)
        textToken = findViewById(R.id.text_token)
        textTrackerName = findViewById(R.id.text_tracker_name)
        lastMessageTextView = findViewById(R.id.last_message_textview)
        startButton = findViewById(R.id.button_start_tracker)
        settingsButton = findViewById(R.id.settingsButton)

        loadDataToViews()

        val developerImageView = findViewById<ImageView>(R.id.developerImageView)
        developerImageView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_and_scale_animation))

        startButton.setOnClickListener { checkPermissionsAndStartTracker() }
        settingsButton.setOnClickListener { openSettings() }
    }

    override fun onResume() {
        super.onResume()
        // ✅ Проверка, режима AUTOorMANUAL при открытии приложения
        if (isAutoMode()) {
            Log.d(TAG, "AUTO MODE активен")

            if (!isServiceRunning(TrackerService::class.java)) {
                Log.d(TAG, "Запускаем TrackerService автоматически")
                //startTracker()
            }
            autoOverlay.visibility = LinearLayout.VISIBLE
        } else {
            autoOverlay.visibility = LinearLayout.GONE
        }



        daysManager.drawDays(daysContainer)
        MusicPlayer.start(this)
        loadDataToViews()
        updateStartButtonState()



        // ✅ Проверка, нужно ли перезапустить трекер с новыми настройками
        if (isServiceRunning(TrackerService::class.java)) {
            Log.d(TAG, "Настройки изменены — перезапускаем TrackerService с новыми данными")

            val intent = Intent(this, TrackerService::class.java).apply {
                putExtra(TrackerService.EXTRA_START_HOUR, TEST_START_HOUR)
                putExtra(TrackerService.EXTRA_END_HOUR, TEST_END_HOUR)
                putExtra(TrackerService.EXTRA_INTERVAL, TEST_INTERVAL_MINUTES)
                putExtra(TrackerService.EXTRA_TOKEN, TOKEN)
                putExtra(TrackerService.EXTRA_TRACKER_NAME, TRACKER_NAME)
            }

            // Останавливаем и сразу запускаем снова
            stopService(intent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ContextCompat.startForegroundService(this, intent)
            else
                startService(intent)

            Toast.makeText(this, "Трекер перезапущен с обновлёнными настройками", Toast.LENGTH_SHORT).show()
        }


        LocalBroadcastManager.getInstance(this).registerReceiver(
            jsonReceiver,
            android.content.IntentFilter(TrackerService.ACTION_UPDATE_MESSAGE)
        )
    }

    override fun onPause() {
        super.onPause()
        MusicPlayer.pause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(jsonReceiver)
    }

    private fun loadDataToViews() {
        val start = prefs.getString("START_HOUR", "08:00") ?: "08:00"
        val end = prefs.getString("END_HOUR", "20:00") ?: "20:00"
        val interval = prefs.getString("INTERVAL", "00:10") ?: "00:10"
        val token = prefs.getString("TOKEN", "SECRET123") ?: "SECRET123"
        val trackerName = prefs.getString("TRACKER_NAME", "Input_TRACKER_NAME") ?: "Input_TRACKER_NAME"


        textStartHour.text = start
        textEndHour.text = end
        textInterval.text = interval
        textToken.text = token
        textTrackerName.text = trackerName

        TEST_START_HOUR = start.split(":")[0].toIntOrNull() ?: 8
        TEST_END_HOUR = end.split(":")[0].toIntOrNull() ?: 20
        TEST_INTERVAL_MINUTES = interval.split(":").getOrNull(1)?.toIntOrNull() ?: 10
        TOKEN = token
        TRACKER_NAME = trackerName

    }

    private fun checkPermissionsAndStartTracker() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            startTracker()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED })
            startTracker()
        else
            Toast.makeText(this, "Выдайте все разрешения для GPS", Toast.LENGTH_LONG).show()
    }

    /** 🚀 Обновлённая логика кнопки СТАРТ */
    private fun startTracker() {
        val intent = Intent(this, TrackerService::class.java).apply {
            putExtra(TrackerService.EXTRA_START_HOUR, TEST_START_HOUR)
            putExtra(TrackerService.EXTRA_END_HOUR, TEST_END_HOUR)
            putExtra(TrackerService.EXTRA_INTERVAL, TEST_INTERVAL_MINUTES)
            putExtra(TrackerService.EXTRA_TOKEN, TOKEN)
            putExtra(TrackerService.EXTRA_TRACKER_NAME, TRACKER_NAME)
        }
        logPendingPoints(this) // ВРЕМЕННЫЙ ТЕСТ ПЯМЯТИ

        if (isServiceRunning(TrackerService::class.java)) {
            // 🔹 Сервис работает → мгновенное снятие координат
            intent.action = TrackerService.ACTION_REQUEST_IMMEDIATE_LOCATION
            startService(intent)
            Toast.makeText(this, "Снятие координат запрошено", Toast.LENGTH_SHORT).show()
            playShortSound(R.raw.data_sound)
        } else {
            // 🔹 Сервис не работает → запускаем
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ContextCompat.startForegroundService(this, intent)
            else
                startService(intent)

            Toast.makeText(this, "Трекер запущен", Toast.LENGTH_SHORT).show()
            playShortSound(R.raw.data_sound)
        }

        updateStartButtonState()
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) return true
        }
        return false
    }

    private fun updateStartButtonState() {
        val running = isServiceRunning(TrackerService::class.java)
        val colorRes = if (running) android.R.color.holo_green_light else android.R.color.darker_gray
        startButton.setBackgroundColor(ContextCompat.getColor(this, colorRes))
    }

    private fun playShortSound(resId: Int) {
        val sound = MediaPlayer.create(this, resId)
        sound.setOnCompletionListener { it.release() }
        sound.start()
    }

    private fun openSettings() {
        playShortSound(R.raw.data_sound)
        val dialog = android.app.AlertDialog.Builder(this)
        dialog.setTitle("Access Settings")
        dialog.setMessage("Enter password to open tracker settings:")

        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        dialog.setView(input)

        dialog.setPositiveButton("OK") { d, _ ->
            val entered = input.text.toString()
            val savedPassword = prefs.getString("PASSWORD", "12345")
            if (entered == savedPassword)
                startActivity(Intent(this, SettingsActivity::class.java))
            else
                Toast.makeText(this, "Wrong password!", Toast.LENGTH_SHORT).show()
            d.dismiss()
        }
        dialog.setNegativeButton("Cancel") { d, _ -> d.cancel() }
        dialog.show()
    }
    private fun logPendingPoints(context: Context) {
        try {
            val prefs = context.getSharedPreferences("send_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString("temp_list", null)

            Log.d("TEST_PREFS", "Сырой JSON из SharedPreferences: $json")

            if (json.isNullOrEmpty()) {
                Log.d("TEST_PREFS", "Офлайн-точек нет (пустая строка или null).")
                return
            }

            val gson = Gson()
            val type = object : TypeToken<List<String>>() {}.type
            val list: List<String> = gson.fromJson(json, type) ?: emptyList()

            if (list.isEmpty()) {
                Log.d("TEST_PREFS", "Офлайн-точек нет (список пуст после парсинга).")
            } else {
                Log.d("TEST_PREFS", "Всего офлайн-точек: ${list.size}")
                list.forEachIndexed { i, point ->
                    Log.d("TEST_PREFS", "Точка ${i + 1}: $point")
                }
            }
        } catch (e: Exception) {
            Log.e("TEST_PREFS", "Ошибка при чтении офлайн-точек: ${e.message}", e)
        }
    }
    private fun isAutoMode(): Boolean {
        val mode = prefs.getString("TRACKING_MODE", "MANUAL")
        return mode == "AUTO"
    }
}





















