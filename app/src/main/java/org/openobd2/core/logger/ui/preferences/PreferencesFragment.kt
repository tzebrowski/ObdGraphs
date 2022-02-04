package org.openobd2.core.logger.ui.preferences

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.GENERIC_MODE

const val NOTIFICATION_GRAPH_VIEW_TOGGLE = "preferences.view.graph.toggle"
const val NOTIFICATION_DEBUG_VIEW_TOGGLE = "preferences.view.debug.toggle"
const val NOTIFICATION_DASH_VIEW_TOGGLE = "preferences.view.dash.toggle"
const val NOTIFICATION_GAUGE_VIEW_TOGGLE = "preferences.view.gauge.toggle"
const val NOTIFICATION_METRICS_VIEW_TOGGLE = "preferences.view.metrics.toggle"


class PreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val onCreateView = super.onCreateView(inflater, container, savedInstanceState)
        registerPrefModeChange()
        registerConnectionModeChange()

        registerCheckboxListener(
            "pref.graph.view.enabled",
            NOTIFICATION_GRAPH_VIEW_TOGGLE
        )

        registerCheckboxListener(
            "pref.debug.view.enabled",
            NOTIFICATION_DEBUG_VIEW_TOGGLE
        )
        registerCheckboxListener(
            "pref.gauge.view.enabled",
            NOTIFICATION_GAUGE_VIEW_TOGGLE
        )
        registerCheckboxListener(
            "pref.dash.view.enabled",
            NOTIFICATION_DASH_VIEW_TOGGLE
        )

        registerCheckboxListener(
            "pref.metrics.view.enabled",
            NOTIFICATION_METRICS_VIEW_TOGGLE
        )

        return onCreateView
    }

    private fun registerCheckboxListener(key: String, actionName: String) {
        val preference = findPreference<CheckBoxPreference>(key)
        preference?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                requireContext().sendBroadcast(Intent().apply {
                    action = actionName
                })
                true
            }
    }

    private fun registerPrefModeChange() {
        val prefMode = findPreference<ListPreference>("selected.connection.type")
        val p1 = findPreference<Preference>("connection.type.bluetooth")
        val p2 = findPreference<Preference>("connection.type.wifi")

        when (Prefs.getString("selected.connection.type")) {
            "bluetooth" -> {
                p1?.isVisible = true
                p2?.isVisible = false
            }
            else -> {
                p1?.isVisible = false
                p2?.isVisible = true
            }
        }

        prefMode?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                when (newValue) {
                    "bluetooth" -> {
                        p1?.isVisible = true
                        p2?.isVisible = false
                    }
                    else -> {
                        p1?.isVisible = false
                        p2?.isVisible = true
                    }
                }
                true
            }
    }


    private fun registerConnectionModeChange() {
        val prefMode = findPreference<ListPreference>("pref.mode")
        val p1 = findPreference<Preference>("pref.pids.generic")
        val p2 = findPreference<Preference>("pref.pids.mode22")

        when (Prefs.getMode()) {
            GENERIC_MODE -> {
                p1?.isVisible = true
                p2?.isVisible = false
            }
            else -> {
                p1?.isVisible = false
                p2?.isVisible = true
            }
        }

        prefMode?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                when (newValue) {
                    GENERIC_MODE -> {
                        p1?.isVisible = true
                        p2?.isVisible = false
                    }
                    else -> {
                        p1?.isVisible = false
                        p2?.isVisible = true
                    }
                }
                true
            }
    }

}