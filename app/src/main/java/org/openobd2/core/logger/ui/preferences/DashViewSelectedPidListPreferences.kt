package org.openobd2.core.logger.ui.preferences

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceManager
import org.openobd2.core.logger.bl.BluetoothConnection
import org.openobd2.core.logger.bl.PidsRegistry
import java.util.*

class DashViewSelectedPidListPreferences(
    context: Context?,
    attrs: AttributeSet?
) :
    MultiSelectListPreference(context, attrs) {
    init {
        val entries: MutableList<CharSequence> =
            LinkedList()
        val entriesValues: MutableList<CharSequence> =
            LinkedList()

        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        when ( pref.getString("pref.mode", "")) {
            "Generic mode" -> {
                PidsRegistry.instance.genericRegistry.definitions.sortedBy { pidDefinition -> pidDefinition.description }
                    .forEach { p ->
                        entries.add(p.description)
                        entriesValues.add(p.pid)
                    }
            }

            else -> {
                PidsRegistry.instance.mode22Registry.definitions.sortedBy { pidDefinition -> pidDefinition.description }
                    .forEach { p ->
                        entries.add(p.description)
                        entriesValues.add(p.pid)
                    }
            }
        }

        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }
}
