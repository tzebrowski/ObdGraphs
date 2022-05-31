package org.openobd2.core.logger.ui.preferences.pid

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.openobd2.core.logger.bl.datalogger.DataLogger
import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.getECUSupportedPids
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
        val pidList = DataLogger.instance.pidDefinitionRegistry().findAll()
        var summary = ""

        Prefs.getECUSupportedPids().forEach {
            pidList.firstOrNull { f -> f.pid == it }?.let { pid ->
                entries.add(pid.description)
                entriesValues.add(pid.description)
                summary += "- ${pid.description}\n"
            }
        }

        setSummary(summary)
        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }
}