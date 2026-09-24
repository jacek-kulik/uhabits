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

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import org.isoron.uhabits.AndroidDirFinder
import org.isoron.uhabits.utils.DatabaseUtils
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException

class AutoBackup(private val context: Context) {

    private val backupPattern = BackupPolicy.pattern(context.packageName)

    fun run(keep: Int = 5) = synchronized(lock) {
        if (Thread.currentThread().isInterrupted) throw InterruptedIOException("Backup interrupted")
        Log.i("AutoBackup", "Starting automatic backups...")
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val uriString = prefs.getString("publicBackupFolder", null)
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            val dir = if (uri.scheme == "content") {
                DocumentFile.fromTreeUri(context, uri)
            } else {
                DocumentFile.fromFile(File(uri.path!!))
            }
            if (dir == null) throw IOException("Backup folder is unavailable")
            runInPublicDir(dir, keep)
            return@synchronized
        }

        val basedir = AndroidDirFinder(context).getFilesDir("Backups")
            ?: throw IOException("Private backup folder is unavailable")
        runInPrivateDir(basedir, keep)
    }

    private fun runInPrivateDir(dir: File, keep: Int) {
        val files = dir.listFiles()?.filter { it.isFile && backupPattern.matches(it.name) }
            ?: throw IOException("Cannot list private backup folder")
        val newestTimestamp = files.maxOfOrNull { it.lastModified() }
        if (BackupPolicy.isDue(newestTimestamp, System.currentTimeMillis())) {
            DatabaseUtils.saveDatabaseCopy(context, dir, automatic = true)
        }
        val current = dir.listFiles()?.filter { it.isFile && backupPattern.matches(it.name) }
            ?: throw IOException("Cannot list private backup folder")
        if (Thread.currentThread().isInterrupted) throw InterruptedIOException("Backup interrupted")
        BackupPolicy.toRemove(current, keep) { it.lastModified() }.forEach { file ->
            if (!file.delete()) Log.e("AutoBackup", "Could not remove old backup: $file")
        }
    }

    private fun runInPublicDir(dir: DocumentFile, keep: Int) {
        val files = dir.listFiles()
            .filter { it.isFile && it.name?.matches(backupPattern) == true }
        val newestTimestamp = files.maxOfOrNull { it.lastModified() }
        if (BackupPolicy.isDue(newestTimestamp, System.currentTimeMillis())) {
            DatabaseUtils.saveDatabaseCopy(context, dir, automatic = true)
        }
        val current = dir.listFiles()
            .filter { it.isFile && it.name?.matches(backupPattern) == true }
        if (Thread.currentThread().isInterrupted) throw InterruptedIOException("Backup interrupted")
        BackupPolicy.toRemove(current, keep) { it.lastModified() }.forEach { file ->
            if (!file.delete()) Log.e("AutoBackup", "Could not remove old backup: ${file.uri}")
        }
    }

    companion object {
        private val lock = Any()
    }
}
