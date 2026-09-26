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

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.isoron.platform.gui.toInt
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.ui.screens.habits.calendar.HabitCompletionCalendar
import org.isoron.uhabits.core.ui.screens.habits.calendar.HabitDayCompletion
import org.isoron.uhabits.inject.HabitsApplicationComponent
import java.text.DateFormatSymbols
import java.util.Locale

class CalendarActivity : AppCompatActivity() {
    private lateinit var yearLabel: TextView
    private lateinit var monthLabel: TextView
    private lateinit var summary: TextView
    private lateinit var yearHeatmap: YearHeatmapView
    private lateinit var monthHeatmap: MonthHeatmapView
    private lateinit var appComponent: HabitsApplicationComponent
    private lateinit var themeSwitcher: AndroidThemeSwitcher
    private var year = getToday().year
    private var month = getToday().month

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            year = it.getInt("calendarYear", year)
            month = it.getInt("calendarMonth", month)
        }
        appComponent = (applicationContext as HabitsApplication).component
        themeSwitcher = AndroidThemeSwitcher(this, appComponent.preferences)
        themeSwitcher.apply()
        setContentView(R.layout.calendar_activity)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setTitle(R.string.calendar)
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        toolbar.setTitleTextColor(Color.WHITE)
        toolbar.navigationIcon?.setTint(Color.WHITE)
        toolbar.setNavigationOnClickListener { finish() }

        yearLabel = findViewById(R.id.calendarYear)
        monthLabel = findViewById(R.id.calendarMonth)
        summary = findViewById(R.id.calendarSummary)
        yearHeatmap = findViewById(R.id.yearHeatmap)
        monthHeatmap = findViewById(R.id.monthHeatmap)
        val navigationTextColor = themeSwitcher.currentTheme.highContrastTextColor.toInt()
        listOf(R.id.previousYear, R.id.nextYear, R.id.previousMonth, R.id.nextMonth).forEach { id ->
            findViewById<Button>(id).setTextColor(navigationTextColor)
        }
        findViewById<Button>(R.id.previousYear).setOnClickListener {
            year--
            refresh()
        }
        findViewById<Button>(R.id.nextYear).setOnClickListener {
            year++
            refresh()
        }
        findViewById<Button>(R.id.previousMonth).setOnClickListener {
            changeMonth(-1)
        }
        findViewById<Button>(R.id.nextMonth).setOnClickListener {
            changeMonth(1)
        }
        refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("calendarYear", year)
        outState.putInt("calendarMonth", month)
        super.onSaveInstanceState(outState)
    }

    private fun changeMonth(delta: Int) {
        val first = LocalDate(year, month, 1)
        val date = if (delta < 0) first.minus(1) else first.plus(first.monthLength)
        year = date.year
        month = date.month
        refresh()
    }

    private fun refresh() {
        val today = getToday()
        val habits = appComponent.habitList.getFiltered(HabitMatcher()).toList()
        val firstWeekday = appComponent.preferences.firstWeekday
        val theme = themeSwitcher.currentTheme
        val accent = theme.color(org.isoron.uhabits.core.models.PaletteColor(3)).toInt()
        val empty = theme.cardBackgroundColor.toInt()
        val text = theme.mediumContrastTextColor.toInt()
        val yearStart = LocalDate(year, 1, 1)
        val yearEnd = LocalDate(year + 1, 1, 1).minus(1)
        val yearDays = HabitCompletionCalendar.calculate(
            habits,
            yearStart,
            if (yearEnd.isNewerThan(today)) today else yearEnd
        )
        yearLabel.text = year.toString()
        yearHeatmap.accentColor = accent
        yearHeatmap.emptyColor = empty
        yearHeatmap.textColor = text
        yearHeatmap.setData(year, yearDays, firstWeekday) { selectedMonth ->
            month = selectedMonth
            refreshMonth(habits, firstWeekday, accent, empty, text, today)
        }
        refreshMonth(habits, firstWeekday, accent, empty, text, today)
    }

    private fun refreshMonth(
        habits: List<Habit>,
        firstWeekday: org.isoron.platform.time.DayOfWeek,
        accent: Int,
        empty: Int,
        text: Int,
        today: LocalDate
    ) {
        val first = LocalDate(year, month, 1)
        val last = first.plus(first.monthLength - 1)
        val monthDays = HabitCompletionCalendar.calculate(
            habits,
            first,
            if (last.isNewerThan(today)) today else last
        )
        monthLabel.text = "${DateFormatSymbols.getInstance(Locale.getDefault()).months[month - 1]} $year"
        monthHeatmap.accentColor = accent
        monthHeatmap.emptyColor = empty
        monthHeatmap.textColor = text
        monthHeatmap.setData(year, month, monthDays, firstWeekday) { selected ->
            if (selected == null || selected.due == 0) {
                showMonthSummary(monthDays)
            } else {
                summary.text = getString(
                    R.string.calendar_day_summary,
                    selected.date.toString(),
                    selected.completed,
                    selected.due
                )
            }
        }
        showMonthSummary(monthDays)
    }

    private fun showMonthSummary(days: List<HabitDayCompletion>) {
        val completed = days.sumOf { it.completed }
        val due = days.sumOf { it.due }
        summary.text = if (due == 0) {
            getString(R.string.calendar_no_habit_history)
        } else {
            getString(R.string.calendar_month_summary, completed, due)
        }
    }
}
