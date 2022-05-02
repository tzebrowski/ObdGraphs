package org.openobd2.core.logger.ui.preferences

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.*
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.datalogger.DataLoggerPreferences
import org.openobd2.core.logger.ui.common.onDoubleClickListener

const val NOTIFICATION_GRAPH_VIEW_TOGGLE = "preferences.view.graph.toggle"
const val NOTIFICATION_DEBUG_VIEW_TOGGLE = "preferences.view.debug.toggle"
const val NOTIFICATION_DASH_VIEW_TOGGLE = "preferences.view.dash.toggle"
const val NOTIFICATION_GAUGE_VIEW_TOGGLE = "preferences.view.gauge.toggle"
const val NOTIFICATION_METRICS_VIEW_TOGGLE = "preferences.view.metrics.toggle"


class PreferencesFragment : PreferenceFragmentCompat() {

    val preferences: DataLoggerPreferences by lazy { DataLoggerPreferences.instance }


    override fun onNavigateToScreen(preferenceScreen: PreferenceScreen?) {
        super.onNavigateToScreen(preferenceScreen)
        setPreferencesFromResource(R.xml.preferences, preferenceScreen!!.key)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (arguments == null) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        } else {
            setPreferencesFromResource(
                R.xml.preferences,
                requireArguments().get("preferences.rootKey") as String
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        registerPrefModeChange()
        listView.setBackgroundColor(Color.LTGRAY)
        registerCheckboxListeners()

        listView.setOnTouchListener(onDoubleClickListener(requireContext()))
        return root
    }

    private fun registerCheckboxListeners() {
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
    }

    private fun registerCheckboxListener(key: String, actionName: String) {
        val preference = findPreference<CheckBoxPreference>(key)
        preference?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                requireContext().sendBroadcast(Intent().apply {
                    action = actionName
                })
                true
            }
    }

    private fun registerPrefModeChange() {
        val prefMode = findPreference<ListPreference>("pref.selected.connection.type")
        val p1 = findPreference<Preference>("connection.type.bluetooth")
        val p2 = findPreference<Preference>("connection.type.wifi")

        when (Prefs.getString("pref.selected.connection.type")) {
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
}