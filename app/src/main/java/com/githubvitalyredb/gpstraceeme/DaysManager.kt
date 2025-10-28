package com.githubvitalyredb.gpstraceeme

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.TextView

class DaysManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "AppPrefs"
        private const val DAYS_KEY = "DAYS_MAP"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var daysMap: MutableMap<String, Int> = mutableMapOf(
        "Mon" to 1, "Tue" to 1, "Wed" to 1,
        "Thu" to 1, "Fri" to 1, "Sat" to 1, "Sun" to 1
    )

    init {
        loadDays()
    }

    fun loadDays() {
        val stored = prefs.getString(DAYS_KEY, null)
        if (stored != null) {
            daysMap.clear()
            stored.split(",").forEach {
                val (k, v) = it.split(":")
                daysMap[k] = v.toInt()
            }
        }
    }

    fun saveDays() {
        val str = daysMap.entries.joinToString(",") { "${it.key}:${it.value}" }
        prefs.edit().putString(DAYS_KEY, str).apply()
    }

    fun drawDays(container: LinearLayout, isEditable: Boolean = false) {
        loadDays()
        container.removeAllViews()
        for ((day, active) in daysMap) {
            val tv = TextView(context).apply {
                text = day
                textSize = 18f
                setPadding(8, 10, 8, 10)
                setBackgroundColor(Color.parseColor("#333333")) // фон всегда серый
                setTextColor(if (active == 1) Color.parseColor("#FFD700") else Color.WHITE) // текст

                if (isEditable) {
                    setOnClickListener {
                        daysMap[day] = if (daysMap[day] == 1) 0 else 1
                        // меняем цвет текста
                        setTextColor(if (daysMap[day] == 1) Color.parseColor("#FFD700") else Color.WHITE)
                        saveDays()
                    }
                }
            }
            container.addView(tv)
        }
    }

}
