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
             it.displayString().let { label ->
                 entries.add(label)
                 entriesValues.add(label)
             }
        }

        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }
}

