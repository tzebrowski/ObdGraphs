package org.obd.graphs.preferences

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.*
import org.obd.graphs.R
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.preferences.dtc.DiagnosticTroubleCodeListPreferences
import org.obd.graphs.preferences.dtc.DiagnosticTroubleCodePreferenceDialog
import org.obd.graphs.preferences.metadata.VehicleMetadataListPreferences
import org.obd.graphs.preferences.metadata.VehicleMetadataPreferenceDialog
import org.obd.graphs.preferences.supported_pids.SupportedPIDsListPreferences
import org.obd.graphs.preferences.supported_pids.SupportedPIDsPreferenceDialog
import org.obd.graphs.preferences.trips.TripsListPreferences
import org.obd.graphs.preferences.trips.TripsPreferenceDialog
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.onDoubleClickListener

const val PREFERENCE_SCREEN_KEY = "preferences.rootKey"
const val PREFS_CONNECTION_TYPE_CHANGED_EVENT = "prefs.connection_type.changed.event"

class PreferencesFragment : PreferenceFragmentCompat() {

    private val onSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            Log.v("Prefs", "Preference $key changed")
        }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        when (preference) {
            is TripsListPreferences -> {
                TripsPreferenceDialog().show(parentFragmentManager, null)
            }
            is VehicleMetadataListPreferences -> {
                VehicleMetadataPreferenceDialog().show(parentFragmentManager, null)
            }

            is SupportedPIDsListPreferences -> {
                SupportedPIDsPreferenceDialog().show(parentFragmentManager, null)
            }

            is DiagnosticTroubleCodeListPreferences -> {
                DiagnosticTroubleCodePreferenceDialog().show(parentFragmentManager, null)
            }

            else -> {
                super.onDisplayPreferenceDialog(preference)
            }
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
        hidePreferences()
        return root
    }

    private fun hidePreferences() {
        findPreference<PreferenceCategory>("pref.dtc.category")?.isVisible =
            DataLogger.instance.isDTCEnabled()
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

        val connectionType = findPreference<ListPreference>(PREFERENCE_CONNECTION_TYPE)
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

        connectionType?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                sendBroadcastEvent(PREFS_CONNECTION_TYPE_CHANGED_EVENT)
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