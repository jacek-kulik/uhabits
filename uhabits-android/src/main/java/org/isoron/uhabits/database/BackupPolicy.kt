package org.isoron.uhabits.database

import org.isoron.platform.time.DateUtils

internal object BackupPolicy {
    fun prefix(packageName: String, automatic: Boolean): String {
        val variant = if (packageName.endsWith(".dev")) " Dev" else ""
        val kind = if (automatic) "Auto Backup" else "Backup"
        return "Loop Habits$variant $kind"
    }

    fun pattern(packageName: String): Regex =
        Regex("^${Regex.escape(prefix(packageName, automatic = true))} .+\\.db$")

    fun isDue(latestModified: Long?, now: Long): Boolean =
        latestModified == null || latestModified > now || now - latestModified >= DateUtils.DAY_LENGTH

    fun <T> toRemove(files: List<T>, keep: Int, lastModified: (T) -> Long): List<T> =
        files.sortedBy(lastModified).dropLast(keep.coerceAtLeast(1))
}
