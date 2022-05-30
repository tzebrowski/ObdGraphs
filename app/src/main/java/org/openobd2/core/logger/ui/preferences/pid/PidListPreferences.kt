package org.openobd2.core.logger.ui.preferences.pid

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import org.openobd2.core.logger.bl.datalogger.DataLogger
import java.util.*


class PidListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    MultiSelectListPreference(context, attrs) {
    private val defaultSelection =
        hashSetOf<String>().apply {
            add("6")  // Engine coolant temperature
            add("12") // Intake manifold absolute pressure
            add("13") // Engine RPM
            add("16") // Intake air temperature
            add("18") // Throttle position
            add("15") // Timing advance
        }

    init {

        val entries: MutableList<CharSequence> =
            LinkedList()
        val entriesValues: MutableList<CharSequence> =
            LinkedList()

        when (getPriority(attrs)) {
            "low" -> {
                lowPriority(entries, entriesValues)
                setDefaultValue(hashSetOf<String>())
            }
            "high" -> {
                highPriority(entries, entriesValues)
                setDefaultValue(defaultSelection)
            }
        }

        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }

    private fun highPriority(
        entries: MutableList<CharSequence>,
        entriesValues: MutableList<CharSequence>
    ) {
        DataLogger.instance.pidDefinitionRegistry().findAll()
            .filter { pidDefinition -> pidDefinition.priority < 4 }
            .sortedBy { pidDefinition -> "[" + pidDefinition.mode + "] " + pidDefinition.description }
            .forEach { p ->
                entries.add("[" + p.mode + "] " + p.description)
                entriesValues.add(p.id.toString())
            }
    }

    private fun lowPriority(
        entries: MutableList<CharSequence>,
        entriesValues: MutableList<CharSequence>
    ) {
        DataLogger.instance.pidDefinitionRegistry().findAll()
            .filter { pidDefinition -> pidDefinition.priority > 4 }
            .sortedBy { pidDefinition -> "[" + pidDefinition.mode + "] " + pidDefinition.description }
            .forEach { p ->
                entries.add("[" + p.mode + "] " + p.description)
                entriesValues.add(p.id.toString())
            }
    }

    private fun getPriority(attrs: AttributeSet?): String {
        return if (attrs == null) {
            ""
        } else {
            val priority: String? = (0 until attrs.attributeCount)
                .filter { index -> attrs.getAttributeName(index) == "priority" }
                .map { index -> attrs.getAttributeValue(index) }.firstOrNull()
            priority ?: ""
        }
    }
}