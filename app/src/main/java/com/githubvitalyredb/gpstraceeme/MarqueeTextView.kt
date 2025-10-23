package com.githubvitalyredb.gpstraceeme

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class MarqueeTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var scrollSpeed = 5f  // скорость прокрутки (пикселей за кадр)
    private var currentScroll = 0f // текущий сдвиг текста по горизонтали
    private var textWidth = 0f     // ширина текста в пикселях
    private val paint = Paint()    // объект для рисования текста

    init {
        isSingleLine = true         // заставляем текст быть в одной строке
        ellipsize = null            // отключаем усечение текста
        setHorizontallyScrolling(true) // включаем горизонтальный скроллинг
        paint.isAntiAlias = true    // сглаживание текста
        paint.textSize = textSize   // берем размер текста из компонента
        paint.color = currentTextColor  // берем цвет текста
    }

    fun setScrollSpeed(speed: Float) {
        scrollSpeed = speed
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        textWidth = paint.measureText(text.toString())
        currentScroll = 0f
    }

    override fun onDraw(canvas: Canvas) {
        val textString = text.toString()

        // Рисуем первый текст с текущим сдвигом (сдвигаем влево)
        canvas.drawText(textString, -currentScroll, baseline.toFloat(), paint)

        // Если текст длиннее виджета, рисуем его же справа для бесшовного повторения
        if (textWidth > width) {
            canvas.drawText(textString, -currentScroll + textWidth + 50f, baseline.toFloat(), paint)
        }

        // Обновляем сдвиг
        currentScroll += scrollSpeed
        if (currentScroll > textWidth + 50f) {
            currentScroll = 0f
        }

        // Просим систему перерисовать view, чтобы анимация продолжалась
        invalidate()
    }

}


