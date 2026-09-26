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
import kotlin.math.ceil

class YearHeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cell = dp(5f)
    private val gap = dp(1f)
    private val labelHeight = dp(20f)
    private var year = 0
    private var firstWeekday = DayOfWeek.SUNDAY
    private var selectedMonth = 1
    private var days: Map<LocalDate, HabitDayCompletion> = emptyMap()
    private var onMonthSelected: ((Int) -> Unit)? = null
    var accentColor: Int = 0xff43a047.toInt()
    var emptyColor: Int = 0xffeeeeee.toInt()
    var textColor: Int = 0xff555555.toInt()

    init {
        isClickable = true
    }

    fun setData(
        year: Int,
        days: List<HabitDayCompletion>,
        firstWeekday: DayOfWeek,
        selectedMonth: Int,
        onMonthSelected: (Int) -> Unit
    ) {
        this.year = year
        this.days = days.associateBy { it.date }
        this.firstWeekday = firstWeekday
        this.selectedMonth = selectedMonth
        this.onMonthSelected = onMonthSelected
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val start = LocalDate(year, 1, 1).startOfWeek(firstWeekday)
        val nextStart = LocalDate(year + 1, 1, 1).startOfWeek(firstWeekday)
        val weeks = ceil(start.daysUntil(nextStart) / 7.0).toInt()
        val width = paddingLeft + paddingRight + weeks * (cell + gap)
        val height = paddingTop + paddingBottom + labelHeight + 7 * (cell + gap)
        setMeasuredDimension(
            resolveSize(width.toInt(), widthMeasureSpec),
            resolveSize(height.toInt(), heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (year == 0) return
        val symbols = DateFormatSymbols.getInstance(Locale.getDefault())
        val gridStart = LocalDate(year, 1, 1).startOfWeek(firstWeekday)
        val nextStart = LocalDate(year + 1, 1, 1).startOfWeek(firstWeekday)
        val weeks = ceil(gridStart.daysUntil(nextStart) / 7.0).toInt()
        paint.textSize = dp(11f)
        paint.color = textColor
        paint.textAlign = Paint.Align.LEFT
        paint.style = Paint.Style.FILL
        for (month in 1..12) {
            val date = LocalDate(year, month, 1)
            val week = gridStart.daysUntil(date) / 7
            canvas.drawText(
                symbols.shortMonths[month - 1],
                paddingLeft + week * (cell + gap).toFloat(),
                paddingTop + paint.textSize,
                paint
            )
        }

        paint.style = Paint.Style.FILL
        val yearEnd = LocalDate(year + 1, 1, 1)
        repeat(weeks * 7) { index ->
            val date = gridStart.plus(index)
            if (date >= yearEnd) return@repeat
            val week = gridStart.daysUntil(date) / 7
            val row = (date.dayOfWeek.daysSinceSunday - firstWeekday.daysSinceSunday + 7) % 7
            val left = paddingLeft + week * (cell + gap).toFloat()
            val top = paddingTop + labelHeight + row * (cell + gap).toFloat()
            val count = days[date]
            val color = colorFor(count?.intensity ?: 0.0, count?.due ?: 0)
            paint.color = color
            val rect = RectF(left, top, left + cell, top + cell)
            canvas.drawRoundRect(rect, dp(2f), dp(2f), paint)
            if (date.month == selectedMonth) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(1f)
                paint.color = accentColor
                canvas.drawRoundRect(rect, dp(2f), dp(2f), paint)
                paint.style = Paint.Style.FILL
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP || year == 0) return true
        performClick()
        val x = event.x - paddingLeft
        val y = event.y - paddingTop - labelHeight
        if (x < 0 || y < 0) return true
        val column = (x / (cell + gap)).toInt()
        val row = (y / (cell + gap)).toInt()
        if (row !in 0..6) return true
        val date = LocalDate(year, 1, 1).startOfWeek(firstWeekday)
            .plus(column * 7 + row)
        if (date.year == year) {
            selectedMonth = date.month
            onMonthSelected?.invoke(date.month)
            invalidate()
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
