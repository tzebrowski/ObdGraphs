package org.openobd2.core.logger.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import org.openobd2.core.logger.bl.datalogger.DataLogger
import org.openobd2.core.logger.bl.datalogger.DataLoggerPreferences
import java.util.*


class PidListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    MultiSelectListPreference(context, attrs) {
    private val preferences: DataLoggerPreferences by lazy { DataLoggerPreferences.instance }
    private val defaultSelection = if (preferences.isGenericModeSelected())  hashSetOf<String>().apply {
        add("6")  // Engine coolant temperature
        add("12") // Intake manifold absolute pressure
        add("13") // Engine RPM
        add("16") // Intake air temperature
        add("18") // Throttle position
        add("15") // Timing advance
        add("9000") // Battery voltage
    } else hashSetOf<String>()

    init {

        val entries: MutableList<CharSequence> =
            LinkedList()
        val entriesValues: MutableList<CharSequence> =
            LinkedList()

        when (getPriority(attrs)){
            "low"-> {
                DataLogger.instance.pidDefinitionRegistry().findAll()
                    .filter { pidDefinition -> pidDefinition.priority > 4}
                    .sortedBy { pidDefinition -> pidDefinition.priority }
                    .forEach { p ->
                        entries.add(p.description)
                        entriesValues.add(p.id.toString())
                    }
                setDefaultValue(hashSetOf<String>())
            }
            "high" -> {
                DataLogger.instance.pidDefinitionRegistry().findAll()
                    .filter { pidDefinition -> pidDefinition.priority < 4}
                    .sortedBy { pidDefinition -> pidDefinition.priority }
                    .forEach { p ->
                        entries.add(p.description)
                        entriesValues.add(p.id.toString())
                    }
                setDefaultValue(defaultSelection)
            }
            else -> {
                DataLogger.instance.pidDefinitionRegistry().findAll()
                   .sortedBy { pidDefinition -> pidDefinition.priority }
                    .forEach { p ->
                        entries.add(p.description)
                        entriesValues.add(p.id.toString())
                    }
                setDefaultValue(defaultSelection)
            }
        }


        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }

    private fun getPriority(attrs: AttributeSet?): String {
        return if ( attrs == null ) {
            ""
        } else {
            val priority: String? = (0 until attrs.attributeCount)
                .filter { index -> attrs.getAttributeName(index) == "priority" }
                .map { index -> attrs.getAttributeValue(index) }.firstOrNull()
            priority?: ""
        }
    }
}