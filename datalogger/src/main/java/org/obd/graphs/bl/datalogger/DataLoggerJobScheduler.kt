package org.obd.graphs.bl.datalogger

import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

internal class DataLoggerJobScheduler {

    private val scheduleService: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private var future: ScheduledFuture<*>? = null

    private val task = Runnable {
        Log.i(LOGGER_TAG, "Start data logging")
        DataLoggerService.start()
    }
    fun stop() {
        Log.i(
            LOGGER_TAG,
            "Canceling Data Logger task"
        )

        future?.cancel(true)
    }

    fun schedule(delay: Long) {

        Log.i(
            LOGGER_TAG,
            "Schedule Data Logger task with the delay: $delay"
        )

        future = scheduleService.schedule(
            task,
            delay,
            TimeUnit.SECONDS
        )
    }
}

