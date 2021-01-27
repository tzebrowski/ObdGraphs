package org.openobd2.core.logger.ui.preferences

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.GENERIC_MODE


class ConfigurationFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val onCreateView = super.onCreateView(inflater, container, savedInstanceState)

        val findPreference = findPreference<ListPreference>("pref.mode")
        val p1 = findPreference<Preference>("pref.pids.generic")
        val p2 = findPreference<Preference>("pref.pids.mode22")


        when (Prefs.getMode(this.requireContext())) {
            GENERIC_MODE -> {
                p1?.isVisible = true
                p2?.isVisible = false
            }
            else -> {
                p1?.isVisible = false
                p2?.isVisible = true
            }
        }

        findPreference?.onPreferenceChangeListener =
            object : Preference.OnPreferenceChangeListener {
                override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
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
                    return true
                }
            }

        return onCreateView
    }

}