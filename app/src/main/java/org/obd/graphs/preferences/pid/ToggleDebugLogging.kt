package org.obd.graphs.preferences.pid

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.CheckBoxPreference
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory

private const val LOGGER_TAG = "ToggleDebugLogging"

class ToggleDebugLogging(
    context: Context?,
    attrs: AttributeSet?
) : CheckBoxPreference(context, attrs) {

    private val loggers =  setOf("org.obd.metrics.transport.StreamConnector")

    init {
        setOnPreferenceChangeListener { _, value ->
           toggleDebugLogging(value as Boolean)
           true
        }
    }

    private fun toggleDebugLogging(value: Boolean) {
        loggers.forEach {
            Log.i(LOGGER_TAG,"Enable debug logging = $value")
            val logger = LoggerFactory.getLogger(it) as Logger
            logger.level = if (value) Level.TRACE else Level.INFO
            logger.isAdditive = false
        }
    }
}