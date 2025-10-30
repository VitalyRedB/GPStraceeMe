package com.githubvitalyredb.gpstraceeme

import android.Manifest
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

class MainActivity : AppCompatActivity() {

    private val TAG = "MAIN_LOG"

    private lateinit var daysManager: DaysManager
    private lateinit var daysContainer: LinearLayout

    companion object {
        private const val PREFS_NAME = "AppPrefs"
        var TEST_START_HOUR = 8
        var TEST_END_HOUR = 20
        var TEST_INTERVAL_MINUTES = 10
        var TOKEN = "SECRET123"
        var USER_ID = "KOD_ID_123"
        const val EXTRA_BACKGROUND_MESSAGES = "EXTRA_BACKGROUND_MESSAGES"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var textStartHour: TextView
    private lateinit var textEndHour: TextView
    private lateinit var textInterval: TextView
    private lateinit var textToken: TextView
    private lateinit var textUserId: TextView
    private lateinit var lastMessageTextView: TextView
    private lateinit var startButton: Button
    private lateinit var settingsButton: Button

    private val jsonReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra(TrackerService.EXTRA_JSON_MESSAGE) ?: "---"
            lastMessageTextView.text = "Last server message: $message"
            lastMessageTextView.setTextColor(android.graphics.Color.YELLOW)
            Log.d(TAG, "Получено сообщение от TrackerService: $message")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Activity onCreate")
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Инициализация менеджера дней
        daysContainer = findViewById(R.id.daysContainer)
        daysManager = DaysManager(this)
        daysManager.drawDays(daysContainer)

        // Инициализация Views
        textStartHour = findViewById(R.id.text_start_hour)
        textEndHour = findViewById(R.id.text_end_hour)
        textInterval = findViewById(R.id.text_interval)
        textToken = findViewById(R.id.text_token)
        textUserId = findViewById(R.id.text_user_id)
        lastMessageTextView = findViewById(R.id.last_message_textview)
        startButton = findViewById(R.id.button_start_tracker)
        settingsButton = findViewById(R.id.settingsButton)

        loadDataToViews()

        // Анимация developerImageView
        val developerImageView = findViewById<ImageView>(R.id.developerImageView)
        val anim = AnimationUtils.loadAnimation(this, R.anim.rotate_and_scale_animation)
        developerImageView.startAnimation(anim)

        // Кнопка запуска трекера
        startButton.setOnClickListener { checkPermissionsAndStartTracker() }

        // Кнопка настроек с проверкой пароля
        settingsButton.setOnClickListener { openSettings() }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity onResume")
        daysManager.drawDays(daysContainer)
        MusicPlayer.start(this)
        loadDataToViews()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            jsonReceiver,
            android.content.IntentFilter(TrackerService.ACTION_UPDATE_MESSAGE)
        )
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Activity onPause")
        MusicPlayer.pause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(jsonReceiver)
    }

    private fun loadDataToViews() {
        val start = prefs.getString("START_HOUR", "08:00") ?: "08:00"
        val end = prefs.getString("END_HOUR", "20:00") ?: "20:00"
        val interval = prefs.getString("INTERVAL", "00:10") ?: "00:10"
        val token = prefs.getString("TOKEN", "SECRET123") ?: "SECRET123"
        val userId = prefs.getString("USER_ID", "YOUR_TRACKER_ID_123") ?: "YOUR_TRACKER_ID_123"

        textStartHour.text = start
        textEndHour.text = end
        textInterval.text = interval
        textToken.text = token
        textUserId.text = userId

        TEST_START_HOUR = start.split(":")[0].toIntOrNull() ?: 8
        TEST_END_HOUR = end.split(":")[0].toIntOrNull() ?: 20
        TEST_INTERVAL_MINUTES = interval.split(":").getOrNull(1)?.toIntOrNull() ?: 10
        TOKEN = token
        USER_ID = userId

        Log.d(TAG, "Настройки загружены: Start=$TEST_START_HOUR, End=$TEST_END_HOUR")
    }

    private fun checkPermissionsAndStartTracker() {
        Log.d(TAG, "Проверка разрешений...")
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            Log.d(TAG, "Не хватает разрешений: $missing. Запрос разрешений...")
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            Log.d(TAG, "Все разрешения даны. Запуск TrackerService...")
            startTracker()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val denied = grantResults.indices.filter { grantResults[it] != PackageManager.PERMISSION_GRANTED }
            if (denied.isEmpty()) {
                Log.d(TAG, "Все разрешения даны. Продолжаем запуск TrackerService...")
                startTracker()
            } else {
                Log.e(TAG, "Не все разрешения даны: ${denied.map { permissions[it] }}")
                Toast.makeText(this, "Нужно выдать все разрешения для GPS", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startTracker() {
        try {
            Log.d(TAG, "Попытка запуска TrackerService...")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val myProcess = activityManager.runningAppProcesses?.find { it.pid == android.os.Process.myPid() }
                val isForeground = myProcess?.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                if (!isForeground) {
                    Log.e(TAG, "Приложение не на экране. ForegroundService не запускается.")
                    Toast.makeText(this, "Откройте приложение на экране", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val backgroundMessagesEnabled = prefs.getBoolean("background_messages_enabled", true)

            val intent = Intent(this, TrackerService::class.java).apply {
                putExtra(TrackerService.EXTRA_START_HOUR, TEST_START_HOUR)
                putExtra(TrackerService.EXTRA_END_HOUR, TEST_END_HOUR)
                putExtra(TrackerService.EXTRA_INTERVAL, TEST_INTERVAL_MINUTES)
                putExtra(TrackerService.EXTRA_TOKEN, TOKEN)
                putExtra(TrackerService.EXTRA_USER_ID, USER_ID)
                putExtra(EXTRA_BACKGROUND_MESSAGES, backgroundMessagesEnabled)
            }

            Log.d(TAG, "Запускаем сервис с параметрами: Start=$TEST_START_HOUR, End=$TEST_END_HOUR, Interval=$TEST_INTERVAL_MINUTES мин")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, intent)
                Log.d(TAG, "startForegroundService вызван")
            } else {
                startService(intent)
                Log.d(TAG, "startService вызван")
            }

            val bgStatus = if (backgroundMessagesEnabled) "уведомления ВКЛ" else "уведомления ВЫКЛ"
            Toast.makeText(
                this,
                "GPStraceeMe запущен ($bgStatus)\nС $TEST_START_HOUR:00 до $TEST_END_HOUR:00 каждые $TEST_INTERVAL_MINUTES мин.",
                Toast.LENGTH_LONG
            ).show()
            playShortSound(R.raw.data_sound)

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка запуска TrackerService: ${e.message}", e)
            Toast.makeText(this, "Ошибка: проверьте параметры трекера", Toast.LENGTH_LONG).show()
        }
    }

    private fun playShortSound(resId: Int) {
        val sound = MediaPlayer.create(this, resId)
        sound.setOnCompletionListener { it.release() }
        sound.start()
    }

    private fun openSettings() {
        playShortSound(R.raw.data_sound)
        val passwordDialog = android.app.AlertDialog.Builder(this)
        passwordDialog.setTitle("Access Settings")
        passwordDialog.setMessage("Enter password to open tracker settings:")

        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        passwordDialog.setView(input)

        passwordDialog.setPositiveButton("OK") { dialog, _ ->
            val entered = input.text.toString()
            val savedPassword = prefs.getString("PASSWORD", "12345")
            if (entered == savedPassword) {
                playShortSound(R.raw.click_sound)
                startActivity(Intent(this, SettingsActivity::class.java))
            } else {
                Toast.makeText(this, "Wrong password!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        passwordDialog.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        passwordDialog.show()
    }
}



















