package org.obd.graphs.preferences.pid

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getStringSet
import org.obd.graphs.sendBroadcastEvent
import java.util.*

private const val HIGH_PRIO_PID_PREF = "pref.pids.generic.high"
private const val LOW_PRIO_PID_PREF = "pref.pids.generic.low"

open class DisplayedPIDsListPreferences(
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
        setOnPreferenceChangeListener { _, _ ->
            sendBroadcastEvent("${key}.event.changed")
            true
        }
    }

    fun highPriority(
        entries: MutableList<CharSequence>,
        entriesValues: MutableList<CharSequence>
    ) {
        val query = Prefs.getStringSet(HIGH_PRIO_PID_PREF)
        dataLogger.getPidDefinitionRegistry().findAll()
            .filter { pidDefinition -> pidDefinition.priority == 0 }
            .filter { pidDefinition -> query.contains(pidDefinition.id.toString()) }
            .sortedBy { p -> p.displayString().toString() }
            .forEach { p ->
                entries.add(p.displayString())
                entriesValues.add(p.id.toString())
            }
    }

    fun lowPriority(
        entries: MutableList<CharSequence>,
        entriesValues: MutableList<CharSequence>
    ) {
        val query = Prefs.getStringSet(LOW_PRIO_PID_PREF)

        dataLogger.getPidDefinitionRegistry().findAll()
            .filter { p -> p.priority > 0 }
            .filter { p -> query.contains(p.id.toString()) }
            .sortedBy { p -> p.displayString().toString() }
            .forEach { p ->
                entries.add(p.displayString())
                entriesValues.add(p.id.toString())
            }
    }
}