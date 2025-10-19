package com.githubvitalyredb.gpstraceeme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "AppPrefs"
        var TEST_START_HOUR = 8
        var TEST_END_HOUR = 20
        var TEST_INTERVAL_MINUTES = 10
        var TOKEN = "SECRET123"
        var USER_ID = "KOD_ID_123"
    }

    private lateinit var textStartHour: TextView
    private lateinit var textEndHour: TextView
    private lateinit var textInterval: TextView
    private lateinit var textToken: TextView
    private lateinit var textUserId: TextView
    private lateinit var lastMessageTextView: TextView
    private lateinit var startButton: Button
    private lateinit var settingsButton: Button
    private lateinit var prefs: SharedPreferences

    // BroadcastReceiver для JSON сообщений
    private val jsonReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra(TrackingService.EXTRA_JSON_MESSAGE) ?: "---"
            lastMessageTextView.text = "Last server message: $message"
            lastMessageTextView.setTextColor(android.graphics.Color.YELLOW) // 🔔 жёлтый цвет
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Инициализация Views
        textStartHour = findViewById(R.id.text_start_hour)
        textEndHour = findViewById(R.id.text_end_hour)
        textInterval = findViewById(R.id.text_interval)
        textToken = findViewById(R.id.text_token)
        textUserId = findViewById(R.id.text_user_id)
        lastMessageTextView = findViewById(R.id.last_message_textview)
        startButton = findViewById(R.id.button_start_tracker)
        settingsButton = findViewById(R.id.settingsButton)

        // Загрузка сохранённых значений
        loadDataToViews()

        // Анимация для developerImageView
        val developerImageView = findViewById<ImageView>(R.id.developerImageView)
        val anim = AnimationUtils.loadAnimation(this, R.anim.rotate_and_scale_animation)
        developerImageView.startAnimation(anim)

        // Кнопка запуска трекера
        startButton.setOnClickListener { startTracker() }

        // Кнопка перехода в настройки с запросом пароля
        settingsButton.setOnClickListener {

            // 🔊 Короткий звук при нажатии
            playShortSound(R.raw.data_sound)

            val passwordDialog = android.app.AlertDialog.Builder(this)
            passwordDialog.setTitle("Access Settings")
            passwordDialog.setMessage("Enter password to open tracker settings:")

            val input = android.widget.EditText(this)
            input.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordDialog.setView(input)

            passwordDialog.setPositiveButton("OK") { dialog, _ ->
                val entered = input.text.toString()
                val savedPassword = prefs.getString("PASSWORD", "12345")
                if (entered == savedPassword) {
                    // 🔊 Звук подтверждения
                    playShortSound(R.raw.click_sound)

                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Wrong password!", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }

            passwordDialog.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            passwordDialog.show()
        }
    }

    override fun onResume() {
        super.onResume()
        MusicPlayer.start(this) // 🔊 Singleton для фоновой музыки
        loadDataToViews()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(jsonReceiver, IntentFilter(TrackingService.ACTION_UPDATE_MESSAGE))
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
    }

    private fun startTracker() {
        try {
            val intent = Intent(this, TrackingService::class.java).apply {
                putExtra(TrackingService.EXTRA_START_HOUR, TEST_START_HOUR)
                putExtra(TrackingService.EXTRA_END_HOUR, TEST_END_HOUR)
                putExtra(TrackingService.EXTRA_INTERVAL, TEST_INTERVAL_MINUTES)
                putExtra(TrackingService.EXTRA_TOKEN, TOKEN)
                putExtra(TrackingService.EXTRA_USER_ID, USER_ID)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, intent)
            } else {
                startService(intent)
            }

            // 🔊 Звук запуска трекера
            playShortSound(R.raw.data_sound)

            Toast.makeText(
                this,
                "GPStraceeMe запущен с $TEST_START_HOUR:00 до $TEST_END_HOUR:00 каждые $TEST_INTERVAL_MINUTES минут",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: проверьте параметры трекера", Toast.LENGTH_LONG).show()
        }
    }

    // Вспомогательный метод для коротких звуков
    private fun playShortSound(resId: Int) {
        val sound = MediaPlayer.create(this, resId)
        sound.setOnCompletionListener { it.release() }
        sound.start()
    }
}
















