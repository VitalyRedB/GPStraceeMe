package com.githubvitalyredb.gpstraceeme

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * Экран настроек трекера.
 * Позволяет задать:
 *  – время начала и окончания трекинга
 *  – интервал отправки данных
 *  – токен, ID пользователя и пароль
 *  – включение/выключение фоновых сообщений (уведомлений)
 */
class SettingsActivity : AppCompatActivity() {

    // 🔧 Элементы интерфейса
    private lateinit var editStartHour: EditText
    private lateinit var editEndHour: EditText
    private lateinit var editInterval: EditText
    private lateinit var editToken: EditText
    private lateinit var editUserId: EditText
    private lateinit var editPassword: EditText
    private lateinit var buttonSaveExit: Button
    private lateinit var switchBackgroundMessages: Switch
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 📂 Загружаем настройки приложения
        prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        // 🧩 Привязываем элементы интерфейса к XML
        editStartHour = findViewById(R.id.edit_start_hour)
        editEndHour = findViewById(R.id.edit_end_hour)
        editInterval = findViewById(R.id.edit_interval)
        editToken = findViewById(R.id.edit_token)
        editUserId = findViewById(R.id.edit_user_id)
        editPassword = findViewById(R.id.edit_password)
        buttonSaveExit = findViewById(R.id.button_start_tracker)
        switchBackgroundMessages = findViewById(R.id.switch_background_messages)

        // 💫 Анимация вращения логотипа разработчика
        val developerImageView = findViewById<ImageView>(R.id.developerImageView)
        val anim = AnimationUtils.loadAnimation(this, R.anim.rotate_and_scale_animation)
        developerImageView.startAnimation(anim)

        // ⏳ Загружаем сохранённые значения в поля
        loadData()

        // 🌙 Инициализация переключателя фоновых уведомлений
        val bgEnabled = prefs.getBoolean("background_messages_enabled", true)
        switchBackgroundMessages.isChecked = bgEnabled

        // 🎚️ Обработчик переключения
        switchBackgroundMessages.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("background_messages_enabled", isChecked).apply()

            // Цвет ползунка в зависимости от состояния
            if (isChecked) {
                // 💡 Включено: ползунок справа, зелёный цвет (#A4C639)
                switchBackgroundMessages.thumbDrawable.setTint(android.graphics.Color.parseColor("#A4C639"))
            } else {
                // 🌑 Выключено: ползунок слева, стандартный серый
                switchBackgroundMessages.thumbDrawable.setTint(
                    getColor(android.R.color.darker_gray)
                )
            }

            // Короткое уведомление пользователю
            Toast.makeText(
                this,
                if (isChecked) "Фоновые сообщения включены" else "Фоновые сообщения выключены",
                Toast.LENGTH_SHORT
            ).show()
        }

        // 📁 Обработка кнопки "Сохранить и выйти"
        buttonSaveExit.setOnClickListener {
            playShortSound(R.raw.click_sound)
            saveData()
        }
    }

    /**
     * Загружает сохранённые параметры из SharedPreferences в поля ввода.
     */
    private fun loadData() {
        editStartHour.setText(prefs.getString("START_HOUR", "08:00"))
        editEndHour.setText(prefs.getString("END_HOUR", "20:00"))
        editInterval.setText(prefs.getString("INTERVAL", "00:10"))
        editToken.setText(prefs.getString("TOKEN", "SECRET123"))
        editUserId.setText(prefs.getString("USER_ID", "YOUR_TRACKER_ID_123"))
        editPassword.setText(prefs.getString("PASSWORD", "12345"))
    }

    /**
     * Сохраняет введённые параметры в SharedPreferences и возвращает в MainActivity.
     */
    private fun saveData() {
        val newStart = editStartHour.text.toString().trim()
        val newEnd = editEndHour.text.toString().trim()
        val newInterval = editInterval.text.toString().trim()
        val newToken = editToken.text.toString().trim()
        val newUserId = editUserId.text.toString().trim()
        val newPassword = editPassword.text.toString().trim()

        // Проверка пароля (5 цифр)
        if (newPassword.length != 5 || !newPassword.all { it.isDigit() }) {
            Toast.makeText(this, "Пароль должен состоять из 5 цифр", Toast.LENGTH_SHORT).show()
            return
        }

        // Сохраняем параметры
        prefs.edit()
            .putString("START_HOUR", newStart)
            .putString("END_HOUR", newEnd)
            .putString("INTERVAL", newInterval)
            .putString("TOKEN", newToken)
            .putString("USER_ID", newUserId)
            .putString("PASSWORD", newPassword)
            .apply()

        Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()

        // Возврат на главный экран
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Воспроизводит короткий звук при нажатии кнопки.
     */
    private fun playShortSound(resId: Int) {
        val sound = MediaPlayer.create(this, resId)
        sound.setOnCompletionListener { it.release() }
        sound.start()
    }

    /**
     * Управление фоновым звуком при возвращении на экран.
     */
    override fun onResume() {
        super.onResume()
        MusicPlayer.start(this)
    }

    /**
     * При сворачивании — ставим музыку на паузу.
     */
    override fun onPause() {
        super.onPause()
        MusicPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}




