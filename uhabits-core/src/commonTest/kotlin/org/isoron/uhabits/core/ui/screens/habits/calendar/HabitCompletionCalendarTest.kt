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
package org.isoron.uhabits.core.ui.screens.habits.calendar

import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.NumericalHabitType
import kotlin.test.Test
import kotlin.test.assertEquals

class HabitCompletionCalendarTest : BaseUnitTest() {
    @Test
    fun `counts due completions and excludes skipped archived and not due habits`() {
        val date = getToday()
        val completed = fixtures.createEmptyHabit().also {
            it.originalEntries.add(Entry(date, Entry.YES_MANUAL))
            it.recompute()
        }
        val missed = fixtures.createEmptyHabit().also {
            it.originalEntries.add(Entry(date, Entry.NO))
            it.recompute()
        }
        val skipped = fixtures.createEmptyHabit().also {
            it.originalEntries.add(Entry(date, Entry.SKIP))
            it.recompute()
        }
        val archived = fixtures.createEmptyHabit().also {
            it.isArchived = true
            it.originalEntries.add(Entry(date, Entry.YES_MANUAL))
            it.recompute()
        }
        val notDue = fixtures.createEmptyHabit().also {
            it.frequency = Frequency(1, 7)
            it.originalEntries.add(Entry(date.minus(1), Entry.YES_MANUAL))
            it.recompute()
        }

        val result = HabitCompletionCalendar.calculate(
            listOf(completed, missed, skipped, archived, notDue),
            date,
            date
        ).single()

        assertEquals(1, result.completed)
        assertEquals(2, result.due)
        assertEquals(0.5, result.intensity)
    }

    @Test
    fun `numerical completion checks both target directions`() {
        val date = getToday()
        val atLeast = fixtures.createEmptyNumericalHabit(NumericalHabitType.AT_LEAST).also {
            it.originalEntries.add(Entry(date, 2000))
            it.recompute()
        }
        val atMost = fixtures.createEmptyNumericalHabit(NumericalHabitType.AT_MOST).also {
            it.targetValue = 2.0
            it.originalEntries.add(Entry(date, 1500))
            it.recompute()
        }
        val smallValue = fixtures.createEmptyNumericalHabit(NumericalHabitType.AT_LEAST).also {
            it.targetValue = 0.001
            it.originalEntries.add(Entry(date, 1))
            it.recompute()
        }

        val result = HabitCompletionCalendar.calculate(
            listOf(atLeast, atMost, smallValue),
            date,
            date
        ).single()

        assertEquals(3, result.completed)
        assertEquals(3, result.due)
    }

    @Test
    fun `days before the first recorded entry are outside a habit history`() {
        val today = getToday()
        val habit = fixtures.createEmptyHabit().also {
            it.originalEntries.add(Entry(today, Entry.YES_MANUAL))
            it.recompute()
        }

        val days = HabitCompletionCalendar.calculate(listOf(habit), today.minus(1), today)

        assertEquals(0, days[0].due)
        assertEquals(1, days[1].due)
        assertEquals(1, days[1].completed)
    }
}
