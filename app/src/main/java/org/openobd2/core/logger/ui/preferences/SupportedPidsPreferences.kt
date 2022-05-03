package org.openobd2.core.logger.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.openobd2.core.logger.bl.datalogger.DataLogger
import java.util.*


class SupportedPidsPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {

    init {

        val entries: MutableList<CharSequence> =
            LinkedList()
        val entriesValues: MutableList<CharSequence> =
            LinkedList()
        val pids = DataLogger.instance.pidDefinitionRegistry().findAll()
        Prefs.getECUSupportedPids().forEach {
            pids.firstOrNull { f -> f.pid == it }?.let { pid ->
                entries.add(pid.description)
                entriesValues.add(pid.description)
            }
        }
        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }
}