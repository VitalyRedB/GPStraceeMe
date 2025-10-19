package com.githubvitalyredb.gpstraceeme

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var editStartHour: EditText
    private lateinit var editEndHour: EditText
    private lateinit var editInterval: EditText
    private lateinit var editToken: EditText
    private lateinit var editUserId: EditText
    private lateinit var editPassword: EditText
    private lateinit var buttonSaveExit: Button
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        // Привязка полей
        editStartHour = findViewById(R.id.edit_start_hour)
        editEndHour = findViewById(R.id.edit_end_hour)
        editInterval = findViewById(R.id.edit_interval)
        editToken = findViewById(R.id.edit_token)
        editUserId = findViewById(R.id.edit_user_id)
        editPassword = findViewById(R.id.edit_password)
        buttonSaveExit = findViewById(R.id.button_start_tracker)

        // Вращение картинки
        val developerImageView = findViewById<ImageView>(R.id.developerImageView)
        val anim = AnimationUtils.loadAnimation(this, R.anim.rotate_and_scale_animation)
        developerImageView.startAnimation(anim)

        // Загружаем текущие значения
        loadData()

        // Обработка нажатия "SAVE and EXIT"
        buttonSaveExit.setOnClickListener {

            // 🔊 короткий звук
            playShortSound(R.raw.click_sound)

            saveData()
        }
    }

    private fun loadData() {
        editStartHour.setText(prefs.getString("START_HOUR", "08:00"))
        editEndHour.setText(prefs.getString("END_HOUR", "20:00"))
        editInterval.setText(prefs.getString("INTERVAL", "00:10"))
        editToken.setText(prefs.getString("TOKEN", "SECRET123"))
        editUserId.setText(prefs.getString("USER_ID", "YOUR_TRACKER_ID_123"))
        editPassword.setText(prefs.getString("PASSWORD", "12345"))
    }

    private fun saveData() {
        val newStart = editStartHour.text.toString().trim()
        val newEnd = editEndHour.text.toString().trim()
        val newInterval = editInterval.text.toString().trim()
        val newToken = editToken.text.toString().trim()
        val newUserId = editUserId.text.toString().trim()
        val newPassword = editPassword.text.toString().trim()

        if (newPassword.length != 5 || !newPassword.all { it.isDigit() }) {
            Toast.makeText(this, "Password must be 5 digits", Toast.LENGTH_SHORT).show()
            return
        }

        prefs.edit()
            .putString("START_HOUR", newStart)
            .putString("END_HOUR", newEnd)
            .putString("INTERVAL", newInterval)
            .putString("TOKEN", newToken)
            .putString("USER_ID", newUserId)
            .putString("PASSWORD", newPassword)
            .apply()

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()

        // Возвращаемся на главный экран
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun playShortSound(resId: Int) {
        val sound = MediaPlayer.create(this, resId)
        sound.setOnCompletionListener { it.release() }
        sound.start()
    }

    override fun onResume() {
        super.onResume()
        MusicPlayer.start(this) // 🔊 Singleton для фоновой музыки
    }

    override fun onPause() {
        super.onPause()
        MusicPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}



