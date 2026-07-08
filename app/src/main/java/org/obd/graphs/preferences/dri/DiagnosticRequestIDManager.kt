package org.obd.graphs.preferences.dri

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateString

data class DiagnosticMappingItem(
    val modeIndex: Int,
    val requestKey: String,
    val headerValue: String
)

object DiagnosticRequestIDManager {
    private const val PREFIX_ID = "pref.adapter.init.mode.id_value.mode_"
    private const val PREFIX_HEADER = "pref.adapter.init.mode.header_value.mode_"

    // Also update the active selected keys if the user edits the currently active one
    private const val ACTIVE_ID_KEY = "pref.adapter.init.mode.id"
    private const val ACTIVE_HEADER_KEY = "pref.adapter.init.mode.header"

    fun getMappings(): List<DiagnosticMappingItem> {
        val mappings = mutableListOf<DiagnosticMappingItem>()
        // Scan through possible indices (assuming max 100 for safety)
        for (i in 1..100) {
            val key = Prefs.getString("$PREFIX_ID$i", null)
            val value = Prefs.getString("$PREFIX_HEADER$i", null)

            if (key != null && value != null) {
                // Remove quotes if they were literally saved in the string
                val cleanKey = key.replace("\"", "")
                val cleanValue = value.replace("\"", "")
                mappings.add(DiagnosticMappingItem(i, cleanKey, cleanValue))
            }
        }
        return mappings
    }

    fun saveMapping(item: DiagnosticMappingItem) {
        val activeId = Prefs.getString(ACTIVE_ID_KEY, "")?.replace("\"", "")

        Prefs.updateString("$PREFIX_ID${item.modeIndex}", item.requestKey)
        Prefs.updateString("$PREFIX_HEADER${item.modeIndex}", item.headerValue)

        // If the user just edited the currently selected active profile, update it globally
        if (activeId == item.requestKey) {
            Prefs.updateString(ACTIVE_HEADER_KEY, item.headerValue)
        }
    }

    fun addMapping(requestKey: String, headerValue: String) {
        // Find the next available index
        val existingIndices = getMappings().map { it.modeIndex }
        val nextIndex = if (existingIndices.isEmpty()) 1 else existingIndices.maxOrNull()!! + 1

        val newItem = DiagnosticMappingItem(nextIndex, requestKey, headerValue)
        saveMapping(newItem)
    }

    fun deleteMapping(item: DiagnosticMappingItem) {
        Prefs.edit()
            .remove("$PREFIX_ID${item.modeIndex}")
            .remove("$PREFIX_HEADER${item.modeIndex}")
            .apply()
    }
}
