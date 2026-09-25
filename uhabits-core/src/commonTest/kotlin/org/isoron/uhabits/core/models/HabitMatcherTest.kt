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
package org.isoron.uhabits.core.models

import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HabitMatcherTest : BaseUnitTest() {

    private fun buildHabit(
        name: String,
        question: String = "",
        description: String = ""
    ): Habit {
        val habit = modelFactory.buildHabit()
        habit.name = name
        habit.question = question
        habit.description = description
        return habit
    }

    @Test
    fun testSearch() {
        val yogaPractice = buildHabit("Yoga practice")
        val running = buildHabit("Running", question = "Did you run today?", description = "daily jog")
        val exercise = buildHabit("Exercise", question = "Did you do yoga today?")
        val mediter = buildHabit("Méditer", description = "mindfulness session")
        val stretching = buildHabit("🧘 Stretching")
        val run10k = buildHabit("10k Run")

        val habits = listOf(yogaPractice, running, exercise, mediter, stretching, run10k)

        // Match by name
        assertMatches(habits, "yoga", listOf(yogaPractice, exercise))

        // Match by question only
        assertMatches(habits, "run", listOf(running, run10k))

        // Match by description only
        assertMatches(habits, "jog", listOf(running))

        // Case-insensitive
        assertMatches(habits, "YOGA", listOf(yogaPractice, exercise))

        // Leading/trailing whitespace
        assertMatches(habits, "  yoga  ", listOf(yogaPractice, exercise))

        // Empty query matches all
        assertMatches(habits, "", listOf(yogaPractice, running, exercise, mediter, stretching, run10k))

        // Whitespace-only query matches all
        assertMatches(habits, "   ", listOf(yogaPractice, running, exercise, mediter, stretching, run10k))

        // Accented character
        assertMatches(habits, "méditer", listOf(mediter))

        // Case-insensitive accented
        assertMatches(habits, "MÉDITER", listOf(mediter))

        // Unaccented query does NOT match accented habit
        assertMatches(habits, "mediter", emptyList())

        // Emoji in habit name doesn't block text match
        assertMatches(habits, "stretching", listOf(stretching))

        // Numbers in name
        assertMatches(habits, "10k", listOf(run10k))

        // Single character
        assertMatches(habits, "y", listOf(yogaPractice, running, exercise))

        // Query longer than all fields
        assertMatches(habits, "a".repeat(100), emptyList())

        // No match
        assertMatches(habits, "swimming", emptyList())
    }

    @Test
    fun testHideEnteredKeepsPartialAtLeastNumericalHabit() {
        val habit = modelFactory.buildHabit()
        habit.type = HabitType.NUMERICAL
        habit.targetType = NumericalHabitType.AT_LEAST
        habit.targetValue = 4.0
        val matcher = HabitMatcher(isEnteredAllowed = false)
        val today = getToday()

        assertTrue(matcher.matches(habit))

        habit.originalEntries.add(Entry(today, 1000))
        habit.recompute()
        assertTrue(habit.isEnteredToday())
        assertFalse(habit.isCompletedToday())
        assertTrue(matcher.matches(habit))
        assertEquals(1000, habit.originalEntries.get(today).value)

        habit.originalEntries.add(Entry(today, 4000))
        habit.recompute()
        assertTrue(habit.isCompletedToday())
        assertFalse(matcher.matches(habit))
    }

    @Test
    fun testHideEnteredHidesSkippedAtLeastNumericalHabit() {
        val habit = modelFactory.buildHabit()
        habit.type = HabitType.NUMERICAL
        habit.targetType = NumericalHabitType.AT_LEAST
        habit.targetValue = 4.0
        habit.originalEntries.add(Entry(getToday(), Entry.SKIP))
        habit.recompute()

        assertTrue(habit.isEnteredToday())
        assertFalse(habit.isCompletedToday())
        assertFalse(HabitMatcher(isEnteredAllowed = false).matches(habit))
    }

    @Test
    fun testHideEnteredStillHidesOtherEnteredHabits() {
        val matcher = HabitMatcher(isEnteredAllowed = false)
        val today = getToday()

        val yesNoHabit = modelFactory.buildHabit()
        yesNoHabit.originalEntries.add(Entry(today, Entry.NO))
        yesNoHabit.recompute()
        assertFalse(matcher.matches(yesNoHabit))

        val atMostHabit = modelFactory.buildHabit()
        atMostHabit.type = HabitType.NUMERICAL
        atMostHabit.targetType = NumericalHabitType.AT_MOST
        atMostHabit.targetValue = 4.0
        atMostHabit.originalEntries.add(Entry(today, 1000))
        atMostHabit.recompute()
        assertFalse(matcher.matches(atMostHabit))
    }

    @Test
    fun testAutomaticCompletionDoesNotHideHabit() {
        val habit = buildHabit("Gym")
        val today = getToday()
        habit.frequency = Frequency(5, 7)
        repeat(5) { offset ->
            habit.originalEntries.add(Entry(today.minus(offset + 1), Entry.YES_MANUAL))
        }
        habit.recompute()

        assertTrue(habit.isCompletedToday())
        assertTrue(HabitMatcher(isCompletedAllowed = false).matches(habit))
        assertTrue(HabitMatcher(isEnteredAllowed = false).matches(habit))

        habit.originalEntries.add(Entry(today, Entry.YES_MANUAL))
        habit.recompute()

        assertFalse(HabitMatcher(isCompletedAllowed = false).matches(habit))

        habit.originalEntries.add(Entry(today, Entry.NO))
        habit.recompute()

        assertFalse(HabitMatcher(isEnteredAllowed = false).matches(habit))
    }

    private fun assertMatches(habits: List<Habit>, query: String, expected: List<Habit>) {
        val matcher = HabitMatcher(searchQuery = query)
        val actual = habits.filter(matcher::matches)
        assertEquals(expected.toSet(), actual.toSet(), "query '$query'")
    }
}
