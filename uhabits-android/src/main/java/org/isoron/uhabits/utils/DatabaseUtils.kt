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
package org.isoron.uhabits.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import org.isoron.platform.time.DateFormats.Companion.getBackupDateFormat
import org.isoron.uhabits.HabitsApplication.Companion.isTestMode
import org.isoron.uhabits.HabitsDatabaseOpener
import org.isoron.uhabits.core.DATABASE_FILENAME
import org.isoron.uhabits.core.DATABASE_VERSION
import org.isoron.uhabits.database.BackupPolicy
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.io.OutputStream

object DatabaseUtils {
    private var opener: HabitsDatabaseOpener? = null

    @JvmStatic
    fun getDatabaseFile(context: Context): File {
        val databaseFilename = databaseFilename
        val root = context.filesDir.path
        return File("$root/../databases/$databaseFilename")
    }

    private val databaseFilename: String
        get() {
            var databaseFilename: String = DATABASE_FILENAME
            if (isTestMode()) databaseFilename = "test.db"
            return databaseFilename
        }

    fun initializeDatabase(context: Context?) {
        opener = HabitsDatabaseOpener(
            context!!,
            databaseFilename,
            DATABASE_VERSION
        )
    }

    @JvmStatic
    @Throws(IOException::class)
    fun saveDatabaseCopy(context: Context, dir: File, automatic: Boolean = false): String {
        val name = backupFilename(context, automatic)
        val destination = File(dir, name)
        if (destination.exists()) throw IOException("Backup already exists: $destination")
        val snapshot = createDatabaseSnapshot(context)
        var pending: File? = null
        try {
            pending = File.createTempFile(".loop-backup-", ".pending", dir)
            copyAndSync(snapshot, pending)
            if (!pending.renameTo(destination)) throw IOException("Cannot publish backup: $destination")
            Log.i("DatabaseUtils", "Wrote: $destination")
            return destination.absolutePath
        } finally {
            pending?.delete()
            snapshot.delete()
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun saveDatabaseCopy(context: Context, dir: DocumentFile, automatic: Boolean = false): String {
        val name = backupFilename(context, automatic)
        if (dir.findFile(name) != null) throw IOException("Backup already exists: $name")
        val snapshot = createDatabaseSnapshot(context)
        var pending: DocumentFile? = null
        try {
            pending = dir.createFile("application/octet-stream", "$name.pending")
                ?: throw IOException("Unable to create backup file")
            FileInputStream(snapshot).use { input ->
                val output = context.contentResolver.openOutputStream(pending.uri, "w")
                    ?: throw IOException("Unable to write backup file")
                output.use {
                    copyWithCancellation(input, it)
                }
            }
            if (!pending.renameTo(name)) throw IOException("Unable to publish backup file")
            if (pending.name != name) throw IOException("Backup file has an unexpected name")
            Log.i("DatabaseUtils", "Wrote: ${pending.uri}")
            return pending.uri.toString()
        } catch (e: Exception) {
            pending?.delete()
            if (e is IOException) throw e
            throw IOException("Could not write backup file", e)
        } finally {
            snapshot.delete()
        }
    }

    private fun backupFilename(context: Context, automatic: Boolean): String {
        val date = getBackupDateFormat().format(System.currentTimeMillis())
        return "${BackupPolicy.prefix(context.packageName, automatic)} $date.db"
    }

    private fun createDatabaseSnapshot(context: Context): File {
        if (Thread.currentThread().isInterrupted) throw InterruptedIOException("Backup interrupted")
        val source = getDatabaseFile(context)
        if (!source.isFile) throw IOException("Database file does not exist")
        val snapshot = File.createTempFile("loop-backup-", ".db", context.cacheDir)
        try {
            SQLiteDatabase.openDatabase(source.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
                .use { database ->
                    // The app disables WAL. An IMMEDIATE transaction blocks writers while
                    // the main database file is copied into a consistent snapshot.
                    database.beginTransactionNonExclusive()
                    try {
                        database.rawQuery("PRAGMA journal_mode", null).use { cursor ->
                            if (cursor.moveToFirst() && cursor.getString(0).equals("wal", true)) {
                                throw IOException("Cannot copy a database in WAL mode")
                            }
                        }
                        copyAndSync(source, snapshot)
                    } finally {
                        database.endTransaction()
                    }
                }
            SQLiteDatabase.openDatabase(snapshot.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                .use { database ->
                    database.rawQuery("PRAGMA quick_check", null).use { cursor ->
                        if (!cursor.moveToFirst() || cursor.getString(0) != "ok") {
                            throw IOException("Database snapshot failed integrity check")
                        }
                    }
                }
            return snapshot
        } catch (e: Exception) {
            snapshot.delete()
            throw IOException("Could not create database snapshot", e)
        }
    }

    private fun copyAndSync(source: File, destination: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                copyWithCancellation(input, output)
                output.fd.sync()
            }
        }
    }

    private fun copyWithCancellation(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(64 * 1024)
        while (true) {
            if (Thread.currentThread().isInterrupted) throw InterruptedIOException("Backup interrupted")
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
        }
    }

    fun openDatabase(): SQLiteDatabase {
        checkNotNull(opener)
        return opener!!.writableDatabase
    }
}
