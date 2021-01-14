package org.openobd2.core.logger.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceManager
import org.openobd2.core.logger.bl.Pids
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
                Pids.instance.generic.definitions.sortedBy { pidDefinition -> pidDefinition.description }
                    .forEach { p ->
                        entries.add(p.description)
                        entriesValues.add(p.pid)
                    }
            }

            else -> {
                Pids.instance.custom.definitions.sortedBy { pidDefinition -> pidDefinition.description }
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
