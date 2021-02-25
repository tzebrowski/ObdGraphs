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

const val NOTIFICATION_DEBUG_VIEW_SHOW = "preferences.view.debug.show"
const val NOTIFICATION_DEBUG_VIEW_HIDE = "preferences.view.debug.hide"

const val NOTIFICATION_DASH_VIEW_SHOW = "preferences.view.dash.show"
const val NOTIFICATION_DASH_VIEW_HIDE = "preferences.view.dash.hide"

const val NOTIFICATION_GAUGE_VIEW_SHOW = "preferences.view.gauge.show"
const val NOTIFICATION_GAUGE_VIEW_HIDE = "preferences.view.gauge.hide"

const val NOTIFICATION_METRICS_VIEW_SHOW = "preferences.view.metrics.show"
const val NOTIFICATION_METRICS_VIEW_HIDE = "preferences.view.metrics.hide"


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
        registerCheckboxListener(
            "pref.debug.view.enabled",
            NOTIFICATION_DEBUG_VIEW_SHOW,
            NOTIFICATION_DEBUG_VIEW_HIDE
        )
        registerCheckboxListener(
            "pref.gauge.view.enabled",
            NOTIFICATION_GAUGE_VIEW_SHOW,
            NOTIFICATION_GAUGE_VIEW_HIDE
        )
        registerCheckboxListener(
            "pref.dash.view.enabled",
            NOTIFICATION_DASH_VIEW_SHOW,
            NOTIFICATION_DASH_VIEW_HIDE
        )

        registerCheckboxListener(
            "pref.metrics.view.enabled",
            NOTIFICATION_METRICS_VIEW_SHOW,
            NOTIFICATION_METRICS_VIEW_HIDE
        )

        return onCreateView
    }

    private fun registerCheckboxListener(key: String, actionTrue: String, actionFalse: String) {
        val preference = findPreference<CheckBoxPreference>(key)
        preference?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                requireContext().sendBroadcast(Intent().apply {
                    action = if (newValue == true) {
                        actionTrue
                    } else {
                        actionFalse
                    }
                })
                true
            }
    }

    private fun registerPrefModeChange() {
        val prefMode = findPreference<ListPreference>("pref.mode")
        val p1 = findPreference<Preference>("pref.pids.generic")
        val p2 = findPreference<Preference>("pref.pids.mode22")

        when (Preferences.getMode(this.requireContext())) {
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