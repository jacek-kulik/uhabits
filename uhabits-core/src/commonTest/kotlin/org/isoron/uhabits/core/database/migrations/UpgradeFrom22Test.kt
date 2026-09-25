package org.isoron.uhabits.core.database.migrations

import kotlinx.coroutines.test.runTest
import org.isoron.platform.io.TestDatabaseHelper
import org.isoron.platform.io.getVersion
import org.isoron.platform.io.migrateTo
import org.isoron.platform.io.query
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.DATABASE_VERSION
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UpgradeFrom22Test : BaseUnitTest() {
    @Test
    fun populatedDatabaseKeepsHabitsAndEntriesThroughLatestMigration() = runTest {
        val db = openDatabaseResource("/databases/022.db")
        try {
            assertEquals(22, db.getVersion())
            val habits = mutableListOf<HabitBefore>()
            db.query("select id, name, description, reminder_hour, reminder_days from Habits order by id") {
                habits.add(HabitBefore(it.getInt(0), it.getText(1), it.getTextOrNull(2), it.getIntOrNull(3), it.getInt(4)))
            }
            val entries = mutableListOf<EntryBefore>()
            db.query("select id, habit, timestamp, value from Repetitions order by id") {
                entries.add(EntryBefore(it.getInt(0), it.getInt(1), it.getLong(2), it.getInt(3)))
            }
            assertEquals(2, habits.size)
            assertEquals(1, entries.size)

            db.migrateTo(DATABASE_VERSION) { TestDatabaseHelper.loadMigrationSQL(it) }
            assertEquals(DATABASE_VERSION, db.getVersion())

            val upgradedHabits = mutableListOf<HabitBefore>()
            db.query("select id, name, question, reminder_hour, reminder_days, description, uuid from Habits order by id") {
                upgradedHabits.add(HabitBefore(it.getInt(0), it.getText(1), it.getTextOrNull(2), it.getIntOrNull(3), it.getInt(4)))
                assertEquals("", it.getText(5))
                assertTrue(assertNotNull(it.getTextOrNull(6)).isNotBlank())
            }
            assertEquals(habits, upgradedHabits)

            val upgradedEntries = mutableListOf<EntryBefore>()
            db.query("select id, habit, timestamp, value, notes from Repetitions order by id") {
                upgradedEntries.add(EntryBefore(it.getInt(0), it.getInt(1), it.getLong(2), it.getInt(3)))
                assertEquals(null, it.getTextOrNull(4))
            }
            assertEquals(entries, upgradedEntries)
        } finally {
            db.close()
        }
    }

    private data class HabitBefore(
        val id: Int,
        val name: String,
        val description: String?,
        val reminderHour: Int?,
        val reminderDays: Int
    )

    private data class EntryBefore(val id: Int, val habitId: Int, val timestamp: Long, val value: Int)
}
