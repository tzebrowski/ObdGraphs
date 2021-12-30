package org.openobd2.core.logger.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import org.openobd2.core.logger.bl.DataLogger
import java.util.*

class PidListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    MultiSelectListPreference(context, attrs) {
    init {

        val entries: MutableList<CharSequence> =
            LinkedList()
        val entriesValues: MutableList<CharSequence> =
            LinkedList()

        DataLogger.INSTANCE.pids().findAll().sortedBy { pidDefinition -> pidDefinition.description }
            .forEach { p ->
                entries.add(p.description)
                entriesValues.add(p.id.toString())
            }

        val default = if (Preferences.getMode(context!!) =="Generic mode")  hashSetOf<String>().apply {
            add("6")  //Engine coolant temperature
            add("12") //Calculated Boost
            add("13") //Engine RPM
            add("16") //Intake air temperature
            add("18") //Throttle position
            add("14") //Vehicle speed
            add("5") //Calculated engine load value
            add("9000") //Battery voltage
        } else hashSetOf<String>()

        setDefaultValue(default)
        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }
}