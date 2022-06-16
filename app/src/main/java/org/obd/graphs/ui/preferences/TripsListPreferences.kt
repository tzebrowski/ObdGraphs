package org.obd.graphs.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.obd.graphs.bl.trip.TripRecorder
import java.util.*


class TripsListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {

    init {

        val entries: MutableList<CharSequence> =
            LinkedList()
        val entriesValues: MutableList<CharSequence> =
            LinkedList()

        entries.add("")
        entriesValues.add("")

        TripRecorder.instance.findAllTripsBy()?.forEach {
            val label = it.substring(5, it.length - 5)
            entries.add(label)
            entriesValues.add(it)
        }

        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }
}