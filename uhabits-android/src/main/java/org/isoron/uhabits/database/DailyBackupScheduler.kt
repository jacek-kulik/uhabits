package org.isoron.uhabits.database

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.util.Log
import org.isoron.platform.time.DateUtils

object DailyBackupScheduler {
    private const val JOB_ID = 92001

    fun schedule(context: Context) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        if (scheduler.getPendingJob(JOB_ID) != null) return

        val job = JobInfo.Builder(JOB_ID, ComponentName(context, DailyBackupJobService::class.java))
            // Check twice a day so a run just before the 24-hour mark does not
            // postpone the next backup for a full extra day.
            .setPeriodic(DateUtils.DAY_LENGTH / 2)
            .setPersisted(true)
            .build()
        if (scheduler.schedule(job) != JobScheduler.RESULT_SUCCESS) {
            Log.e("DailyBackupScheduler", "Could not schedule daily backups")
        }
    }
}
