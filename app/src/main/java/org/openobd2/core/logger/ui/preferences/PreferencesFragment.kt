package org.openobd2.core.logger.ui.preferences

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.*
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.datalogger.DataLoggerPreferences
import org.openobd2.core.logger.ui.common.onDoubleClickListener

const val PREFERENCE_SCREEN_KEY = "preferences.rootKey"

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
                requireArguments().get(PREFERENCE_SCREEN_KEY) as String
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)

        registerViewsPreferenceChangeListeners()
        registerConnectionTypeListener()
        registerProfileListener()
        registerSaveUserPreferences()
        listView.setBackgroundColor(Color.LTGRAY)
        listView.setOnTouchListener(onDoubleClickListener(requireContext()))
        return root
    }

    private fun registerConnectionTypeListener() {
        val bluetooth = "bluetooth"

        val prefMode = findPreference<ListPreference>(PREFERENCE_CONNECTION_TYPE)
        val p1 = findPreference<Preference>("$PREFERENCE_CONNECTION_TYPE.$bluetooth")
        val p2 = findPreference<Preference>("$PREFERENCE_CONNECTION_TYPE.wifi")

        when (Prefs.getString(PREFERENCE_CONNECTION_TYPE)) {
            bluetooth -> {
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
                    bluetooth -> {
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