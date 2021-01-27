package org.openobd2.core.logger.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import org.openobd2.core.logger.bl.DataLoggerService
import java.util.*

class GaugeViewSelectedPidListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    MultiSelectListPreference(context, attrs) {
    init {
        val entries: MutableList<CharSequence> =
            LinkedList()
        val entriesValues: MutableList<CharSequence> =
            LinkedList()

        context?.let {
            DataLoggerService.dataLogger.pidRegistry(it).definitions.sortedBy { pidDefinition -> pidDefinition.description }
                .forEach { p ->
                    entries.add(p.description)
                    entriesValues.add(p.pid)
                }
        }

        val default = hashSetOf<String>().apply {
            add("05")//Engine coolant temperature
            add("0B") //Intake manifold absolute pressure
            add("0C") //Engine RPM
            add("0F") //Intake air temperature
            add("11") //Throttle position
        }

        setDefaultValue(default)

        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }
}
