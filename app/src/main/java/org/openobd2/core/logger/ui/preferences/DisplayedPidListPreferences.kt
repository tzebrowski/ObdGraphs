package org.openobd2.core.logger.ui.preferences

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.MultiSelectListPreference
import org.openobd2.core.logger.bl.datalogger.DataLogger
import java.util.*


class DisplayedPidListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    MultiSelectListPreference(context, attrs) {

    init {

        val entries: MutableList<CharSequence> =
            LinkedList()
        val entriesValues: MutableList<CharSequence> =
            LinkedList()

        highPriority(entries, entriesValues)
        entries.add("----------------------------------------------------------------------")
        entriesValues.add("-1")
        lowPriority(entries, entriesValues)

        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }

    private fun highPriority(
        entries: MutableList<CharSequence>,
        entriesValues: MutableList<CharSequence>
    ) {
        val query = Prefs.getStringSet("pref.pids.generic.high")
        Log.e("eee", "query $query")
        DataLogger.instance.pidDefinitionRegistry().findAll()
            .filter { pidDefinition -> pidDefinition.priority < 4 }
            .filter { pidDefinition -> query.contains(pidDefinition.id.toString()) }
            .sortedBy { pidDefinition -> "[" + pidDefinition.mode + "] " + pidDefinition.description }
            .forEach { p ->
                entries.add("[" + p.mode + "] " + p.description)
                entriesValues.add(p.id.toString())
            }
    }

    private fun lowPriority(
        entries: MutableList<CharSequence>,
        entriesValues: MutableList<CharSequence>
    ) {
        val query = Prefs.getStringSet("pref.pids.generic.low")

        DataLogger.instance.pidDefinitionRegistry().findAll()
            .filter { pidDefinition -> pidDefinition.priority > 4 }
            .filter { pidDefinition -> query.contains(pidDefinition.id.toString()) }
            .sortedBy { pidDefinition -> "[" + pidDefinition.mode + "] " + pidDefinition.description }
            .forEach { p ->
                entries.add("[" + p.mode + "] " + p.description)
                entriesValues.add(p.id.toString())
            }
    }
}