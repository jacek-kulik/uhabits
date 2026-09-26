/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.regression

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.LargeTest
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.BaseUserInterfaceTest
import org.isoron.uhabits.R
import org.isoron.uhabits.acceptance.steps.CommonSteps.launchApp
import org.isoron.uhabits.activities.about.AboutActivity
import org.isoron.uhabits.activities.calendar.CalendarActivity
import org.junit.Test
import java.lang.Thread.sleep
import java.text.DateFormatSymbols
import java.util.Locale

@LargeTest
class SavedStateTest : BaseUserInterfaceTest() {

    /**
     * Make sure that the main activity can be recreated by using
     * BundleSavedState after being destroyed. See bug:
     * https://github.com/iSoron/uhabits/issues/287
     */
    @Test
    @Throws(Exception::class)
    fun testBundleSavedState() {
        launchApp()
        startActivity(AboutActivity::class.java)
        sleep(1000)
        device.pressBack()
    }

    @Test
    fun calendar_selection_survives_rotation() {
        val today = getToday()
        val selected = LocalDate(today.year - 1, today.month, 1).minus(1)
        startActivity(CalendarActivity::class.java)
        onView(withId(R.id.previousYear)).perform(click())
        onView(withId(R.id.previousMonth)).perform(click())

        rotateDevice()

        onView(withId(R.id.calendarYear)).check(matches(withText(selected.year.toString())))
        val monthName = DateFormatSymbols.getInstance(Locale.getDefault()).months[selected.month - 1]
        onView(withId(R.id.calendarMonth)).check(matches(withText("$monthName ${selected.year}")))
    }
}
