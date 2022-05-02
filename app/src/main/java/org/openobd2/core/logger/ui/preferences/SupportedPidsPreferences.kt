package org.openobd2.core.logger.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import org.openobd2.core.logger.bl.datalogger.DataLogger
import java.util.*


class SupportedPidsPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    MultiSelectListPreference(context, attrs) {

    init {

        val entries: MutableList<CharSequence> =
            LinkedList()
        val entriesValues: MutableList<CharSequence> =
            LinkedList()

        val pidDefinitionRegistry = DataLogger.instance.pidDefinitionRegistry()

        Prefs.getECUSupportedPids().forEach { it ->
            val pid = pidDefinitionRegistry.findBy(it)
            entries.add(it)
            entriesValues.add(it)
        }

        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }
}