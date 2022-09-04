package org.obd.graphs.preferences.pid

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.obd.graphs.preferences.VehicleProperty
import org.obd.graphs.preferences.getVehicleProperties
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import java.util.*

class VehiclePreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {

    init {
        initialize()
    }

    private fun initialize() {
        val entries: MutableList<CharSequence> =
            LinkedList()
        val entriesValues: MutableList<CharSequence> =
            LinkedList()

        getVehicleProperties().forEach { p ->
            val text = p.displayString()
            entries.add(text)
            entriesValues.add(text)
        }

        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }
}

fun VehicleProperty.displayString(): Spanned {
    val text = "[$name] $value"
    return SpannableString(text).apply {
        val endIndexOf = text.indexOf("]") + 1
        setSpan(
            RelativeSizeSpan(0.5f), 0, endIndexOf,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        setSpan(
            ForegroundColorSpan(COLOR_PHILIPPINE_GREEN), 0, endIndexOf,
            0
        )
    }
}

