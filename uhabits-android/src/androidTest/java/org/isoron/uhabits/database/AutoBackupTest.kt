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

package org.isoron.uhabits.database

import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.preference.PreferenceManager
import androidx.test.filters.MediumTest
import org.isoron.platform.time.DateUtils
import org.isoron.uhabits.AndroidDirFinder
import org.isoron.uhabits.BaseAndroidTest
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

@MediumTest
class AutoBackupTest : BaseAndroidTest() {
    @Test
    fun testRun() {
        val basedir = AndroidDirFinder(targetContext).getFilesDir("Backups")!!
        removeAllFiles(basedir)
        val prefix = BackupPolicy.prefix(targetContext.packageName, automatic = true)
        val oldFiles = (1..6).map { k ->
            File(basedir, "$prefix test-$k.db").apply {
                FileOutputStream(this).close()
                setLastModified(DateUtils.DAY_LENGTH * k)
            }
        }
        val manual = File(
            basedir,
            "${BackupPolicy.prefix(targetContext.packageName, automatic = false)} manual.db"
        )
        val otherPackage = if (targetContext.packageName.endsWith(".dev")) {
            "org.isoron.uhabits"
        } else {
            "org.isoron.uhabits.dev"
        }
        val otherInstall = File(
            basedir,
            "${BackupPolicy.prefix(otherPackage, automatic = true)} other.db"
        )
        val unrelated = File(basedir, "notes.txt")
        listOf(manual, otherInstall, unrelated).forEach { FileOutputStream(it).close() }

        val autoBackup = AutoBackup(targetContext)
        autoBackup.run(keep = 5)

        oldFiles.take(2).forEach { assertFalse(it.exists()) }
        oldFiles.drop(2).forEach { assertTrue(it.exists()) }
        listOf(manual, otherInstall, unrelated).forEach { assertTrue(it.exists()) }

        val backups = basedir.listFiles()!!.filter {
            BackupPolicy.pattern(targetContext.packageName).matches(it.name)
        }
        assertEquals(5, backups.size)
        val latest = backups.maxBy { it.lastModified() }
        SQLiteDatabase.openDatabase(latest.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("PRAGMA quick_check", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("ok", cursor.getString(0))
            }
        }
    }

    @Test
    fun testRunWithEmptyDir() {
        val basedir = AndroidDirFinder(targetContext).getFilesDir("Backups")!!
        removeAllFiles(basedir)
        basedir.delete()

        val autoBackup = AutoBackup(targetContext)
        autoBackup.run()
        val backupCount = basedir.listFiles()!!.count {
            BackupPolicy.pattern(targetContext.packageName).matches(it.name)
        }
        assertEquals(1, backupCount)
    }

    @Test
    fun testFreshPrivateBackupsStillPrune() {
        val dir = AndroidDirFinder(targetContext).getFilesDir("Backups")!!
        assertFreshBackupsArePruned(dir)
    }

    @Test
    fun testFreshPublicBackupsStillPrune() {
        val dir = File(targetContext.cacheDir, "test-public-backups")
        assertTrue(dir.isDirectory || dir.mkdirs())
        PreferenceManager.getDefaultSharedPreferences(targetContext).edit()
            .putString("publicBackupFolder", Uri.fromFile(dir).toString())
            .apply()
        assertFreshBackupsArePruned(dir)
    }

    private fun assertFreshBackupsArePruned(dir: File) {
        removeAllFiles(dir)
        val prefix = BackupPolicy.prefix(targetContext.packageName, automatic = true)
        val now = System.currentTimeMillis()
        val files = (1..6).map { k ->
            File(dir, "$prefix fresh-$k.db").apply {
                FileOutputStream(this).close()
                assertTrue(setLastModified(now - 60_000 + k * 1000))
            }
        }

        AutoBackup(targetContext).run(keep = 5)

        assertFalse(files.first().exists())
        files.drop(1).forEach { assertTrue(it.exists()) }
        assertEquals(files.drop(1).map { it.name }.toSet(), dir.listFiles()!!.map { it.name }.toSet())
    }

    private fun removeAllFiles(dir: File) {
        dir.list().forEach { path ->
            val file = File("${dir.path}/$path")
            assertTrue(file.delete())
        }
    }
}
