package org.obd.graphs.preferences.pid

import android.content.Context
import android.util.AttributeSet
import org.obd.graphs.ui.gauge.gaugeVirtualScreen
import java.util.*
import kotlin.collections.HashSet

class DisplayedPIDsVirtualScreenListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    DisplayedPIDsListPreferences(context, attrs) {
    init {

        val entries: MutableList<CharSequence> =
            LinkedList()
        val entriesValues: MutableList<CharSequence> =
            LinkedList()

        highPriority(entries, entriesValues)
        lowPriority(entries, entriesValues)
        setEntries(entries.toTypedArray())

        entryValues = entriesValues.toTypedArray()

        onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            val newList = newValue as HashSet<String>
            gaugeVirtualScreen.updateVirtualScreen(newList.toList())
            true
        }
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        values = gaugeVirtualScreen.getVirtualScreenMetrics()
    }
}