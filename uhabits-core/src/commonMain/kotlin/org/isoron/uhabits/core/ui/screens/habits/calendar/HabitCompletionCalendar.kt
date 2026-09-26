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
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.core.ui.screens.habits.calendar

import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.NumericalHabitType

data class HabitDayCompletion(
    val date: LocalDate,
    val completed: Int,
    val due: Int
) {
    val intensity: Double
        get() = if (due == 0) 0.0 else completed.toDouble() / due
}

object HabitCompletionCalendar {
    fun calculate(
        habits: List<Habit>,
        from: LocalDate,
        to: LocalDate
    ): List<HabitDayCompletion> {
        if (from.isNewerThan(to)) return emptyList()

        val trackedHabits = habits
            .filter { !it.isArchived }
            .mapNotNull { habit ->
                val oldestEntry = habit.computedEntries.getKnown().lastOrNull()?.date
                oldestEntry?.let { habit to it }
            }

        val result = mutableListOf<HabitDayCompletion>()
        var date = from
        while (date <= to) {
            var completed = 0
            var due = 0
            trackedHabits.forEach { (habit, oldestEntry) ->
                if (date < oldestEntry) return@forEach

                val value = habit.computedEntries.get(date).value
                if (value == Entry.SKIP || (!habit.isNumerical && value == Entry.YES_AUTO)) {
                    return@forEach
                }

                due++
                if (isCompleted(habit, value)) completed++
            }
            result.add(HabitDayCompletion(date, completed, due))
            date = date.plus(1)
        }
        return result
    }

    private fun isCompleted(habit: Habit, value: Int): Boolean {
        if (habit.type != HabitType.NUMERICAL) {
            return value == Entry.YES_MANUAL
        }
        if (value == Entry.UNKNOWN) return false

        val valueAsDouble = value / 1000.0
        return when (habit.targetType) {
            NumericalHabitType.AT_LEAST -> valueAsDouble >= habit.targetValue
            NumericalHabitType.AT_MOST -> valueAsDouble <= habit.targetValue
        }
    }
}
