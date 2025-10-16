package com.githubvitalyredb.gpstraceeme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.*
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.media.MediaPlayer

class MainActivity : AppCompatActivity() {

    companion object {
        var TEST_START_HOUR = 8
        var TEST_END_HOUR = 20
        var TEST_INTERVAL_MINUTES = 10
        var TOKEN = "SECRET123"
        var USER_ID = "KOD_ID_123"
    }

    private lateinit var editStartHour: EditText
    private lateinit var editEndHour: EditText
    private lateinit var editInterval: EditText
    private lateinit var editToken: EditText
    private lateinit var editUserId: EditText
    private lateinit var lastMessageTextView: TextView
    private lateinit var startButton: Button

    private lateinit var mediaPlayer: MediaPlayer

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val jsonMessage = intent?.getStringExtra(TrackingService.EXTRA_JSON_MESSAGE)
            jsonMessage?.let { lastMessageTextView.text = it }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editStartHour = findViewById(R.id.edit_start_hour)
        editEndHour = findViewById(R.id.edit_end_hour)
        editInterval = findViewById(R.id.edit_interval)
        editToken = findViewById(R.id.edit_token)
        editUserId = findViewById(R.id.edit_user_id)
        lastMessageTextView = findViewById(R.id.last_message_textview)
        startButton = findViewById(R.id.button_start_tracker)

        // Подставляем текущие значения по умолчанию
        editStartHour.setText(String.format("%02d:00", TEST_START_HOUR))
        editEndHour.setText(String.format("%02d:00", TEST_END_HOUR))
        editInterval.setText(String.format("00:%02d", TEST_INTERVAL_MINUTES))
        editToken.setText(TOKEN)
        editUserId.setText(USER_ID)

        // Маска ввода HH:mm
        setupTimeMask(editStartHour)
        setupTimeMask(editEndHour)
        setupTimeMask(editInterval)

        // Загружаем анимацию из XML, Запускаем анимацию
        val developerImageView = findViewById<ImageView>(R.id.developerImageView)
        val anim = AnimationUtils.loadAnimation(this, R.anim.rotate_and_scale_animation)
        developerImageView.startAnimation(anim)

        // Фоновая музыка через MediaPlayer
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music)
        mediaPlayer.isLooping = true   // чтобы музыка играла бесконечно
        mediaPlayer.setVolume(1f, 1f)  // максимальная громкость
        mediaPlayer.start()

        startButton.setOnClickListener {
            playSound(R.raw.data_sound)
            try {
                val startParts = editStartHour.text.toString().split(":")
                val endParts = editEndHour.text.toString().split(":")
                val intervalParts = editInterval.text.toString().split(":")

                val startHour = startParts[0].toInt()
                val endHour = endParts[0].toInt()
                val intervalMinutes = intervalParts[0].toInt() * 60 + intervalParts[1].toInt()

                val token = editToken.text.toString()
                val userId = editUserId.text.toString()

                // Сохраняем глобально
                TEST_START_HOUR = startHour
                TEST_END_HOUR = endHour
                TEST_INTERVAL_MINUTES = intervalMinutes
                TOKEN = token
                USER_ID = userId

                // Запуск TrackingService
                val intent = Intent(this, TrackingService::class.java).apply {
                    putExtra(TrackingService.EXTRA_START_HOUR, startHour)
                    putExtra(TrackingService.EXTRA_END_HOUR, endHour)
                    putExtra(TrackingService.EXTRA_INTERVAL, intervalMinutes)
                    putExtra(TrackingService.EXTRA_TOKEN, token)
                    putExtra(TrackingService.EXTRA_USER_ID, userId)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(this, intent)
                } else {
                    startService(intent)
                }

                Toast.makeText(this, "GPStraceeMe запущен с $startHour:00 до $endHour:00 каждые $intervalMinutes минут", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: проверьте формат времени HH:mm", Toast.LENGTH_LONG).show()
            }
        }

        // Регистрируем LocalBroadcast для получения последних сообщений
        LocalBroadcastManager.getInstance(this).registerReceiver(
            messageReceiver,
            IntentFilter(TrackingService.ACTION_UPDATE_MESSAGE)
        )
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer.pause()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
        mediaPlayer.release()
    }


    private fun playSound(resId: Int) {
        val soundEffectPlayer = MediaPlayer.create(this, resId)
        soundEffectPlayer.setOnCompletionListener { mp -> mp.release() }
        soundEffectPlayer.start()
    }

    private fun setupTimeMask(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    if (it.length == 2 && !it.contains(":")) {
                        it.append(":")
                        editText.setSelection(it.length)
                    }
                }
            }
        })
    }
}









