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
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import org.isoron.uhabits.R
import java.io.File
import java.io.IOException

/** Copies an already-generated export to a document chosen by the user. */
class SaveExportActivity : AppCompatActivity() {
    private val createDocument = registerForActivityResult(StartActivityForResult()) { result ->
        val destination = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && destination != null) {
            copyExport(destination)
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return

        val source = intent.getStringExtra(EXTRA_SOURCE)
        if (source == null) {
            finish()
            return
        }
        val name = sourceName(source)
        try {
            createDocument.launch(
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = exportMimeType(name)
                    putExtra(Intent.EXTRA_TITLE, name)
                }
            )
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun sourceName(source: String): String {
        val uri = Uri.parse(source)
        if (uri.scheme != "content") {
            return File(if (uri.scheme == "file") uri.path!! else source).name
        }
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)?.let { return File(it).name }
                }
            }
        } catch (e: Exception) {
            Log.w("SaveExportActivity", "Could not read export filename", e)
        }
        return File(uri.lastPathSegment ?: "").name.ifBlank { "habits-export.db" }
    }

    private fun copyExport(destination: Uri) {
        val source = intent.getStringExtra(EXTRA_SOURCE) ?: run {
            finish()
            return
        }
        setContentView(
            FrameLayout(this).apply {
                addView(
                    ProgressBar(this@SaveExportActivity),
                    FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER)
                )
            }
        )
        Thread {
            val saved = try {
                val uri = Uri.parse(source)
                val input = if (uri.scheme == "content") {
                    contentResolver.openInputStream(uri)
                } else {
                    File(if (uri.scheme == "file") uri.path!! else source).inputStream()
                } ?: throw IOException("Could not open export")
                input.use { stream ->
                    val output = contentResolver.openOutputStream(destination, "w")
                        ?: throw IOException("Could not open destination")
                    output.use { stream.copyTo(it) }
                }
                true
            } catch (e: Exception) {
                Log.e("SaveExportActivity", "Could not save export", e)
                // ACTION_CREATE_DOCUMENT has already created the file. Avoid leaving a broken backup.
                try {
                    DocumentsContract.deleteDocument(contentResolver, destination)
                } catch (cleanupError: Exception) {
                    Log.w("SaveExportActivity", "Could not remove incomplete export", cleanupError)
                }
                false
            }
            runOnUiThread {
                Toast.makeText(
                    this,
                    if (saved) R.string.export_saved else R.string.could_not_export,
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }.start()
    }

    companion object {
        const val EXTRA_SOURCE = "org.isoron.uhabits.EXTRA_EXPORT_SOURCE"
    }
}

internal fun exportMimeType(filename: String): String =
    if (filename.endsWith(".zip", ignoreCase = true)) "application/zip" else "application/octet-stream"
