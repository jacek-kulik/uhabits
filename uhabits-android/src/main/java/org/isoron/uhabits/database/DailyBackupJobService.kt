package org.isoron.uhabits.database

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.Future

class DailyBackupJobService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var running: Future<*>? = null
    private var stopped = false

    override fun onStartJob(params: JobParameters): Boolean {
        stopped = false
        running = executor.submit {
            val retry = try {
                AutoBackup(applicationContext).run()
                false
            } catch (e: Exception) {
                Log.e("DailyBackupJobService", "Backup failed", e)
                true
            }
            mainHandler.post {
                if (!stopped) jobFinished(params, retry)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        stopped = true
        running?.cancel(true)
        running = null
        return true
    }

    override fun onDestroy() {
        stopped = true
        executor.shutdownNow()
        super.onDestroy()
    }
}
