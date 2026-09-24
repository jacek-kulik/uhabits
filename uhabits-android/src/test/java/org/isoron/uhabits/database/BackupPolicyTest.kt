package org.isoron.uhabits.database

import org.isoron.platform.time.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPolicyTest {
    @Test
    fun usesSeparateBackupNamesForDev() {
        val release = BackupPolicy.pattern("org.isoron.uhabits")
        val dev = BackupPolicy.pattern("org.isoron.uhabits.dev")

        assertEquals("Loop Habits Backup", BackupPolicy.prefix("org.isoron.uhabits", false))
        assertEquals("Loop Habits Dev Backup", BackupPolicy.prefix("org.isoron.uhabits.dev", false))
        assertTrue(release.matches("Loop Habits Auto Backup 2026-09-24 120000.db"))
        assertFalse(release.matches("Loop Habits Backup 2026-09-24 120000.db"))
        assertFalse(release.matches("Loop Habits Dev Auto Backup 2026-09-24 120000.db"))
        assertTrue(dev.matches("Loop Habits Dev Auto Backup 2026-09-24 120000.db"))
        assertFalse(dev.matches("Loop Habits Dev Backup 2026-09-24 120000.db"))
        assertFalse(dev.matches("Loop Habits Auto Backup 2026-09-24 120000.db"))
    }

    @Test
    fun backupIsDueAfterOneDay() {
        val now = 1_000_000_000L
        assertTrue(BackupPolicy.isDue(null, now))
        assertFalse(BackupPolicy.isDue(now - DateUtils.DAY_LENGTH + 1, now))
        assertTrue(BackupPolicy.isDue(now - DateUtils.DAY_LENGTH, now))
        assertTrue(BackupPolicy.isDue(now + 1, now))
    }

    @Test
    fun retentionKeepsTheNewestCopies() {
        val timestamps = listOf(4L, 1L, 6L, 2L, 5L, 3L)
        assertEquals(listOf(1L), BackupPolicy.toRemove(timestamps, 5) { it })
        assertEquals(emptyList<Long>(), BackupPolicy.toRemove(timestamps, 6) { it })
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), BackupPolicy.toRemove(timestamps, 0) { it })
    }
}
