package org.openobd2.core.logger.ui.preferences.header

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.ListPreference
import org.openobd2.core.logger.ui.preferences.Prefs

class HeaderListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {

    init {
        val values = linkedMapOf(
            "" to "",
            "DA10F1" to "DA10F1",
            "DB33F1" to "DB33F1",
            "7DF" to "7DF"
        )
        val numberOfHeaders = Prefs.getInt(CAN_HEADER_COUNTER_PREF, 0)

        Log.d(LOG_KEY, "Number of custom CAN headers available: $numberOfHeaders")

        if (numberOfHeaders > 0){
            (1 .. numberOfHeaders).forEach {
                val header = Prefs.getString("pref.adapter.init.header.$it", "").toString()
                values[header] = header
            }
        }

        setDefaultValue("")
        entries = values.values.toTypedArray()
        entryValues = values.keys.toTypedArray()
    }
}