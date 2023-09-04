package org.obd.graphs.ui.graph

import org.obd.graphs.preferences.*

private const val VIRTUAL_SCREEN_SELECTION = "pref.graph.virtual.selected"
const val PREF_GRAPH_DIALOG = "pref.graph.pids.selected"

class GraphVirtualScreen {
    fun getCurrentVirtualScreen() = Prefs.getString(VIRTUAL_SCREEN_SELECTION, "1")!!

    fun updateVirtualScreen(screenId: String) {
        Prefs.updateString(VIRTUAL_SCREEN_SELECTION, screenId)
    }

    fun getVirtualScreenMetrics(): Set<String> =
        Prefs.getStringSet(getVirtualScreenPrefKey(), mutableSetOf())!!

    fun getVirtualScreenPrefKey(): String = "$PREF_GRAPH_DIALOG.${getCurrentVirtualScreen()}"
}

val graphVirtualScreen = GraphVirtualScreen()