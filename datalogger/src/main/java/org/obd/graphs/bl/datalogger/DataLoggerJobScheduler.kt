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
        Log.i(LOGGER_TAG, "Starting data logger task....")
        dataLogger.start()
    }
    fun stop() {
        Log.i(
            LOGGER_TAG,
            "Canceling data logger scheduled task"
        )

        future?.cancel(true)
    }

    fun schedule(delay: Long) {

        Log.i(
            LOGGER_TAG,
            "Schedule data logger task with the delay=${delay}s"
        )

        future = scheduleService.schedule(
            task,
            delay,
            TimeUnit.SECONDS
        )
    }
}

