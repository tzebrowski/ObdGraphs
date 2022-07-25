package org.obd.graphs.ui.preferences

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import org.obd.graphs.R
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.bl.datalogger.DataLoggerPreferences
import org.obd.graphs.ui.common.onDoubleClickListener
import org.obd.graphs.ui.preferences.trips.TripsListPreferences
import org.obd.graphs.ui.preferences.trips.TripsPreferenceDialog

const val PREFERENCE_SCREEN_KEY = "preferences.rootKey"

class PreferencesFragment : PreferenceFragmentCompat() {

    private val onSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            Log.v("Prefs", "Preference $key changed")
        }

    val preferences: DataLoggerPreferences by lazy { DataLoggerPreferences.instance }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (preference is TripsListPreferences) {         // rounded language dialog
            TripsPreferenceDialog().show(parentFragmentManager, null)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onNavigateToScreen(preferenceScreen: PreferenceScreen?) {
        super.onNavigateToScreen(preferenceScreen)
        setPreferencesFromResource(R.xml.preferences, preferenceScreen!!.key)
        registerListeners()
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

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(
            onSharedPreferenceChangeListener
        )
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(
            onSharedPreferenceChangeListener
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        registerListeners()
        listView.setBackgroundColor(Color.LTGRAY)
        listView.setOnTouchListener(onDoubleClickListener(requireContext()))
        return root
    }

    private fun registerListeners() {
        registerConnectionTypeListener()
        registerViewsPreferenceChangeListeners()

        findPreference<ListPreference>("pref.profile.max_profiles")?.let {
            it.setOnPreferenceChangeListener { _, _ ->
                navigateToPreferencesScreen("pref.profiles")
                true
            }
        }
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