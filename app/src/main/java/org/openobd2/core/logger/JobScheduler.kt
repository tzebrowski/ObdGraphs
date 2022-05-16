package org.openobd2.core.logger

import android.util.Log
import org.openobd2.core.logger.bl.datalogger.DataLoggerService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private val scheduleService: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
private val dataLoggerTask = Runnable {
    Log.i(ACTIVITY_LOGGER_TAG, "Start data logging")
    DataLoggerService.startAction()
}

private const val CONNECT_TASK_DELAY_S = 5L

fun scheduleDataLogger() {
    Log.i(
        ACTIVITY_LOGGER_TAG,
        "Schedule connect task WITH delay: $CONNECT_TASK_DELAY_S"
    )
    scheduleService.schedule(dataLoggerTask, CONNECT_TASK_DELAY_S, TimeUnit.SECONDS)
}