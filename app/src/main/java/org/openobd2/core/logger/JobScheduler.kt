package org.openobd2.core.logger

import android.util.Log
import org.openobd2.core.logger.bl.datalogger.DataLoggerService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private val scheduleService: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
private val dataLoggerTask = Runnable {
    Log.i(ACTIVITY_LOGGER_TAG, "Start data logging")
    DataLoggerService.start()
}

fun scheduleDataLogger() {
    val powerPreferences: PowerPreferences = getPowerPreferences()

    Log.i(
        ACTIVITY_LOGGER_TAG,
        "Schedule connect task WITH delay: ${powerPreferences.startDataLoggingAfter}"
    )
    scheduleService.schedule(
        dataLoggerTask,
        powerPreferences.startDataLoggingAfter,
        TimeUnit.SECONDS
    )
}