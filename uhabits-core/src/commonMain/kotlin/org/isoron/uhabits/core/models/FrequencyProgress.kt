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

enum class FrequencyProgressPeriod {
    THIS_WEEK,
    THIS_MONTH,
    CUSTOM_DAYS
}

data class FrequencyProgress(
    val completed: Int,
    val target: Int,
    val period: FrequencyProgressPeriod,
    val periodDays: Int = 0
) {
    companion object {
        fun calculate(
            frequency: Frequency,
            entries: EntryList,
            today: LocalDate,
            firstWeekday: DayOfWeek
        ): FrequencyProgress? {
            if (frequency.denominator == 1) return null

            val (from, period) = when (frequency.denominator) {
                7 -> today.startOfWeek(firstWeekday) to FrequencyProgressPeriod.THIS_WEEK
                30, 31 -> today.startOfMonth() to FrequencyProgressPeriod.THIS_MONTH
                else -> today.minus(frequency.denominator - 1) to FrequencyProgressPeriod.CUSTOM_DAYS
            }
            val completed = entries.getKnown().count {
                it.value == Entry.YES_MANUAL && it.date >= from && it.date <= today
            }
            return FrequencyProgress(
                completed = completed,
                target = frequency.numerator,
                period = period,
                periodDays = if (period == FrequencyProgressPeriod.CUSTOM_DAYS) {
                    frequency.denominator
                } else {
                    0
                }
            )
        }
    }
}
