/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.activities.calendar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.core.ui.screens.habits.calendar.HabitDayCompletion
import java.text.DateFormatSymbols
import java.util.Locale

class MonthHeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var year = 0
    private var month = 1
    private var firstWeekday = DayOfWeek.SUNDAY
    private var days: Map<LocalDate, HabitDayCompletion> = emptyMap()
    private var onDateSelected: ((HabitDayCompletion?) -> Unit)? = null
    var accentColor: Int = 0xff43a047.toInt()
    var emptyColor: Int = 0xffeeeeee.toInt()
    var textColor: Int = 0xff555555.toInt()

    init {
        isClickable = true
    }

    fun setData(
        year: Int,
        month: Int,
        days: List<HabitDayCompletion>,
        firstWeekday: DayOfWeek,
        onDateSelected: (HabitDayCompletion?) -> Unit
    ) {
        this.year = year
        this.month = month
        this.days = days.associateBy { it.date }
        this.firstWeekday = firstWeekday
        this.onDateSelected = onDateSelected
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = width / 7 * 7
        setMeasuredDimension(width, resolveSize(height, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (year == 0) return
        val columnWidth = width / 7f
        val rowHeight = height / 7f
        val symbols = DateFormatSymbols.getInstance(Locale.getDefault()).shortWeekdays
        paint.color = textColor
        paint.textSize = dp(12f)
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL
        repeat(7) { column ->
            val dayIndex = (firstWeekday.daysSinceSunday + column) % 7
            canvas.drawText(symbols[dayIndex + 1], (column + 0.5f) * columnWidth, rowHeight * 0.65f, paint)
        }

        val firstDate = LocalDate(year, month, 1)
        val offset = (firstDate.dayOfWeek.daysSinceSunday - firstWeekday.daysSinceSunday + 7) % 7
        val monthLength = firstDate.monthLength
        for (day in 1..monthLength) {
            val index = offset + day - 1
            val column = index % 7
            val row = index / 7 + 1
            val left = column * columnWidth
            val top = row * rowHeight
            val date = LocalDate(year, month, day)
            val count = days[date]
            paint.color = colorFor(count?.intensity ?: 0.0, count?.due ?: 0)
            paint.style = Paint.Style.FILL
            val inset = dp(3f)
            canvas.drawRoundRect(
                RectF(left + inset, top + inset, left + columnWidth - inset, top + rowHeight - inset),
                dp(4f),
                dp(4f),
                paint
            )
            paint.color = textColor
            paint.textSize = dp(14f)
            canvas.drawText(day.toString(), left + columnWidth / 2, top + rowHeight * 0.63f, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP || year == 0) return true
        performClick()
        val column = (event.x / (width / 7f)).toInt().coerceIn(0, 6)
        val row = (event.y / (height / 7f)).toInt() - 1
        val firstDate = LocalDate(year, month, 1)
        val offset = (firstDate.dayOfWeek.daysSinceSunday - firstWeekday.daysSinceSunday + 7) % 7
        val day = row * 7 + column - offset + 1
        if (day in 1..firstDate.monthLength) {
            onDateSelected?.invoke(days[LocalDate(year, month, day)])
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun colorFor(intensity: Double, due: Int): Int {
        if (due == 0) return emptyColor
        val level = when {
            intensity <= 0.0 -> 0.15
            intensity < 0.34 -> 0.38
            intensity < 0.67 -> 0.62
            intensity < 1.0 -> 0.82
            else -> 1.0
        }
        return ColorUtils.blendARGB(emptyColor, accentColor, level.toFloat())
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
}
