package org.obd.graphs.ui.preferences

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.obd.graphs.bl.trip.TripDesc
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
             displayString(it).let { label ->
                 entries.add(label)
                 entriesValues.add(label)
             }
        }

        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }
}

private fun displayString(tripDesc: TripDesc): Spanned {
    val text = "[profile: ${tripDesc.profileLabel}] ${tripDesc.startTime} (${tripDesc.tripTimeSec}s)"

    return SpannableString(text).apply {
        setSpan(
            RelativeSizeSpan(0.5f), 0, text.indexOf("]") + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        setSpan(
            ForegroundColorSpan(Color.parseColor("#C22636")),
            text.indexOf("("),
            text.indexOf(")") + 1,
            0
        )
    }
}