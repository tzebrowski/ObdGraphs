package org.obd.graphs.bl.datalogger

import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import org.obd.graphs.getContext

private const val SCHEDULED_ACTION_START = "org.obd.graphs.logger.scheduled.START"
private const val SCHEDULED_ACTION_STOP = "org.obd.graphs.logger.scheduled.STOP"
private const val ACTION_START = "org.obd.graphs.logger.START"
private const val ACTION_STOP = "org.obd.graphs.logger.STOP"
private const val SCHEDULED_START_DELAY = "org.obd.graphs.logger.scheduled.delay"

class DataLoggerService : JobIntentService() {
    private val jobScheduler = DataLoggerJobScheduler()

    override fun onHandleWork(intent: Intent) {
        when (intent.action) {
            ACTION_START -> {
                dataLogger.start()
            }
            ACTION_STOP -> {
                dataLogger.stop()
            }

            SCHEDULED_ACTION_STOP -> {
                jobScheduler.stop()
            }

            SCHEDULED_ACTION_START -> {
                val delay = intent.extras?.getLong(SCHEDULED_START_DELAY)
                jobScheduler.schedule(delay as Long)
            }
        }
    }

    companion object {
        @JvmStatic
        fun scheduleStart(delay: Long) {
            enqueueWork(SCHEDULED_ACTION_START) {
                it.putExtra(SCHEDULED_START_DELAY, delay)
            }
        }

        @JvmStatic
        fun scheduledStop() {
            enqueueWork(SCHEDULED_ACTION_STOP)
        }

        @JvmStatic
        fun start() {
            enqueueWork(ACTION_START)
        }

        @JvmStatic
        fun stop() {
            enqueueWork(ACTION_STOP)
        }

        private fun enqueueWork(intentAction: String, func: (p: Intent) -> Unit = {}) {
            try {
                getContext()?.run {
                    val intent = Intent(this, DataLoggerService::class.java)
                        .apply { action = intentAction }
                        .apply { putExtra("init", 1) }

                    func(intent)
                    enqueueWork(
                        this,
                        DataLoggerService::class.java,
                        1, intent
                    )
                }
            } catch (e: IllegalStateException) {
                Log.e("DataLoggerService", "Failed to enqueue the work", e)
            }
        }
    }
}