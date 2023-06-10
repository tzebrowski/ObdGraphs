package org.obd.graphs.preferences.pid

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.CheckBoxPreference
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

private const val LOGGER_TAG = "ToggleObdMetricsDebugLogging"

class ToggleObdMetricsDebugLogging(
    context: Context?,
    attrs: AttributeSet?
) : CheckBoxPreference(context, attrs) {

    private val loggers =  setOf(
        "org.obd.metrics.transport.StreamConnector",
        "org.obd.metrics.api.DefaultWorkflow")

    init {
        setOnPreferenceChangeListener { _, value ->
           toggleDebugLogging(value as Boolean)
           true
        }
    }

    private fun toggleDebugLogging(value: Boolean) {
        loggers.forEach {
            val logger = LoggerFactory.getLogger(it) as Logger
            logger.level = if (value) Level.TRACE else Level.INFO
            Log.i(LOGGER_TAG,"Enabled debug logging = $value for $it, isDebugEnabled=${logger.isDebugEnabled}, isTraceEnabled=${logger.isTraceEnabled}")
        }
    }
}