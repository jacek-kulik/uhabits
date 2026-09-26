/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.core.models

import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class FrequencyProgressTest {
    private val today = LocalDate(2026, 6, 10)

    @Test
    fun countsCurrentCalendarWeekFromConfiguredFirstDay() {
        val entries = EntryList().apply {
            add(Entry(today.minus(3), Entry.YES_MANUAL))
            add(Entry(today.minus(2), Entry.YES_MANUAL))
            add(Entry(today.minus(1), Entry.YES_AUTO))
            add(Entry(today, Entry.YES_MANUAL))
            add(Entry(today.plus(1), Entry.YES_MANUAL))
        }

        val mondayStart = FrequencyProgress.calculate(
            Frequency(3, 7),
            entries,
            today,
            DayOfWeek.MONDAY
        )
        val sundayStart = FrequencyProgress.calculate(
            Frequency(3, 7),
            entries,
            today,
            DayOfWeek.SUNDAY
        )

        assertEquals(FrequencyProgress(2, 3, FrequencyProgressPeriod.THIS_WEEK), mondayStart)
        assertEquals(FrequencyProgress(3, 3, FrequencyProgressPeriod.THIS_WEEK), sundayStart)
    }

    @Test
    fun countsCurrentCalendarMonth() {
        val entries = EntryList().apply {
            add(Entry(today.minus(9), Entry.YES_MANUAL))
            add(Entry(today, Entry.YES_MANUAL))
            add(Entry(LocalDate(2026, 5, 31), Entry.YES_MANUAL))
        }

        val actual = FrequencyProgress.calculate(
            Frequency(2, 30),
            entries,
            today,
            DayOfWeek.MONDAY
        )

        assertEquals(FrequencyProgress(2, 2, FrequencyProgressPeriod.THIS_MONTH), actual)
    }

    @Test
    fun countsManualCompletionsInRollingCustomDayWindow() {
        val entries = EntryList().apply {
            add(Entry(today.minus(3), Entry.YES_MANUAL))
            add(Entry(today.minus(2), Entry.YES_MANUAL))
            add(Entry(today.minus(1), Entry.YES_AUTO))
            add(Entry(today, Entry.SKIP))
            add(Entry(today.plus(1), Entry.YES_MANUAL))
        }

        val actual = FrequencyProgress.calculate(
            Frequency(1, 3),
            entries,
            today,
            DayOfWeek.MONDAY
        )

        assertEquals(
            FrequencyProgress(1, 1, FrequencyProgressPeriod.CUSTOM_DAYS, 3),
            actual
        )
    }

    @Test
    fun dailyProgressIncludesOnlyToday() {
        val entries = EntryList().apply {
            add(Entry(today.minus(1), Entry.YES_MANUAL))
            add(Entry(today, Entry.YES_MANUAL))
        }

        val actual = FrequencyProgress.calculate(
            Frequency.DAILY,
            entries,
            today,
            DayOfWeek.MONDAY
        )

        assertEquals(FrequencyProgress(1, 1, FrequencyProgressPeriod.TODAY), actual)
    }
}
