package org.openobd2.core.logger.ui.preferences

import android.content.Intent
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import org.openobd2.core.logger.*

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
    updateToolbar(NOTIFICATION_GRAPH_VIEW_TOGGLE)
    updateToolbar(NOTIFICATION_DASH_VIEW_TOGGLE)
    updateToolbar(NOTIFICATION_GAUGE_VIEW_TOGGLE)
    updateToolbar(NOTIFICATION_METRICS_VIEW_TOGGLE)
}

private fun updateToolbar(notificationId: String) {
    ApplicationContext.get()?.let{
        it.sendBroadcast(Intent().apply {
            action = notificationId
        })
    }
}

private fun PreferencesFragment.registerCheckboxListener(key: String, actionName: String) {
    val preference = findPreference<CheckBoxPreference>(key)
    preference?.onPreferenceChangeListener =
        Preference.OnPreferenceChangeListener { _, _ ->
            requireContext().sendBroadcast(Intent().apply {
                action = actionName
            })
            true
        }
}