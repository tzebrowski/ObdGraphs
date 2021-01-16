package org.openobd2.core.logger.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import org.openobd2.core.logger.bl.DataLoggerService
import java.util.*

class Mode22PidListPreferences(
        context: Context?,
        attrs: AttributeSet?
) :
        MultiSelectListPreference(context, attrs) {
    init {
        val entries: MutableList<CharSequence> =
                LinkedList()
        val entriesValues: MutableList<CharSequence> =
                LinkedList()

        DataLoggerService.dataLogger.mode22.registry.definitions.sortedBy { pidDefinition -> pidDefinition.description }
                .forEach { p ->
                    entries.add(p.description)
                    entriesValues.add(p.pid)
                }

        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }
}