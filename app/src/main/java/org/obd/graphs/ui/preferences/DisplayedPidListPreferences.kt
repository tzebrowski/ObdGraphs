package org.obd.graphs.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.metrics.pid.PidDefinition
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
        lowPriority(entries, entriesValues)

        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }

    private fun highPriority(
        entries: MutableList<CharSequence>,
        entriesValues: MutableList<CharSequence>
    ) {
        val query = Prefs.getStringSet("pref.pids.generic.high")
        DataLogger.instance.pidDefinitionRegistry().findAll()
            .filter { pidDefinition -> pidDefinition.priority < 4 }
            .filter { pidDefinition -> query.contains(pidDefinition.id.toString()) }
            .sortedBy { p -> p.displayString().toString() }
            .forEach { p ->
                entries.add(p.displayString())
                entriesValues.add(p.id.toString())
            }
    }

    private fun lowPriority(
        entries: MutableList<CharSequence>,
        entriesValues: MutableList<CharSequence>
    ) {
        val query = Prefs.getStringSet("pref.pids.generic.low")

        DataLogger.instance.pidDefinitionRegistry().findAll()
            .filter { p -> p.priority > 4 }
            .filter { p -> query.contains(p.id.toString()) }
            .sortedBy { p -> p.displayString().toString() }
            .forEach { p ->
                entries.add(p.displayString())
                entriesValues.add(p.id.toString())
            }
    }
}