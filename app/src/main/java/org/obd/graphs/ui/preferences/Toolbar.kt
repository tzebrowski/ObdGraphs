package org.obd.graphs.ui.preferences

import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import org.obd.graphs.*

const val PREFERENCE_CONNECTION_TYPE = "pref.adapter.connection.type"

internal fun PreferencesFragment.registerViewsPreferenceChangeListeners() {
    registerCheckboxListener(
        GRAPH_VIEW_ID,
        NOTIFICATION_GRAPH_VIEW_TOGGLE
    )
    registerCheckboxListener(
        GAUGE_VIEW_ID,
        NOTIFICATION_GAUGE_VIEW_TOGGLE
    )
    registerCheckboxListener(
        DASH_VIEW_ID,
        NOTIFICATION_DASH_VIEW_TOGGLE
    )

    registerCheckboxListener(
        METRICS_VIEW_ID,
        NOTIFICATION_METRICS_VIEW_TOGGLE
    )
}

fun updateToolbar() {
    sendBroadcastEvent(NOTIFICATION_GRAPH_VIEW_TOGGLE)
    sendBroadcastEvent(NOTIFICATION_DASH_VIEW_TOGGLE)
    sendBroadcastEvent(NOTIFICATION_GAUGE_VIEW_TOGGLE)
    sendBroadcastEvent(NOTIFICATION_METRICS_VIEW_TOGGLE)
}

private fun PreferencesFragment.registerCheckboxListener(key: String, actionName: String) {
    val preference = findPreference<CheckBoxPreference>(key)
    preference?.onPreferenceChangeListener =
        Preference.OnPreferenceChangeListener { _, _ ->
            sendBroadcastEvent(actionName)
            true
        }
}