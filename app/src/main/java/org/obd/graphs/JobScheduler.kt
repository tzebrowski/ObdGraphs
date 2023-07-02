package org.obd.graphs

import android.util.Log
import org.obd.graphs.activity.LOG_TAG
import org.obd.graphs.bl.datalogger.DataLoggerService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class JobScheduler {
    private val scheduleService: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private val dataLoggerTask = Runnable {
        Log.i(LOG_TAG, "Start data logging")
        DataLoggerService.start()
    }

    fun scheduleDataLogger() {
        val powerPreferences: PowerPreferences = getPowerPreferences()

        Log.i(
            LOG_TAG,
            "Schedule connect task WITH delay: ${powerPreferences.startDataLoggingAfter}"
        )
        scheduleService.schedule(
            dataLoggerTask,
            powerPreferences.startDataLoggingAfter,
            TimeUnit.SECONDS
        )
    }
}

val jobScheduler = JobScheduler()

