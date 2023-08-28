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
import org.obd.graphs.activity.navigateToScreen
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.preferences.dtc.DiagnosticTroubleCodeListPreferences
import org.obd.graphs.preferences.dtc.DiagnosticTroubleCodePreferenceDialog
import org.obd.graphs.preferences.metadata.VehicleMetadataListPreferences
import org.obd.graphs.preferences.metadata.VehicleMetadataPreferenceDialog
import org.obd.graphs.preferences.pid.PIDsListPreferences
import org.obd.graphs.preferences.pid.PIDsListPreferenceDialog
import org.obd.graphs.preferences.trips.TripsListPreferences
import org.obd.graphs.preferences.trips.TripsPreferenceDialog
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.onDoubleClickListener
import org.obd.graphs.ui.gauge.PREF_GAUGE_DIALOG
import org.obd.graphs.ui.graph.PREF_GRAPH_DIALOG

const val PREFERENCE_SCREEN_KEY = "preferences.rootKey"
const val PREFS_CONNECTION_TYPE_CHANGED_EVENT = "prefs.connection_type.changed.event"

const val PREF_GAUGE_RECORDINGS = "pref.gauge.recordings"
const val PREF_GAUGE_DISPLAYED_PARAMETERS_IDS = "pref.gauge.displayed_parameter_ids"
const val PREF_GRAPH_DISPLAYED_PARAMETERS_IDS = "pref.graph.displayed_parameter_ids"
const val PREF_GIULIA_DISPLAYED_PARAMETERS_IDS = "pref.giulia.displayed_parameter_ids"


private const val LOG_KEY = "Prefs"
class PreferencesFragment : PreferenceFragmentCompat() {

    private val onSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            Log.v(LOG_KEY, "Preference $key changed")
        }
    override fun onDisplayPreferenceDialog(preference: Preference?) {
        when (preference) {
            is TripsListPreferences -> {
                TripsPreferenceDialog().show(parentFragmentManager, null)
            }
            is VehicleMetadataListPreferences -> {
                VehicleMetadataPreferenceDialog().show(parentFragmentManager, null)
            }

            is PIDsListPreferences -> {
               PIDsListPreferenceDialog(preference.key, preference.prio)
                   .show(parentFragmentManager, null)
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

            val preferenceKey = requireArguments().get(PREFERENCE_SCREEN_KEY) as String

            Log.d(LOG_KEY, "Loading Pref Screen for key=$preferenceKey")

            setPreferencesFromResource(
                R.xml.preferences,
                preferenceKey
            )

            openPreferenceDialogFor(preferenceKey)
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
            dataLogger.isDTCEnabled()
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
        val p3 = findPreference<Preference>("$PREFERENCE_CONNECTION_TYPE.usb")


        when (Prefs.getString(PREFERENCE_CONNECTION_TYPE)) {
            bluetooth -> {
                p1?.isVisible = true
                p2?.isVisible = false
                p3?.isVisible = false

            }
            "wifi" -> {
                p1?.isVisible = false
                p2?.isVisible = true
                p3?.isVisible = false
            }

            "usb" -> {
                p1?.isVisible = false
                p2?.isVisible = false
                p3?.isVisible = true
            }

            else -> {

            }
        }

        connectionType?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                sendBroadcastEvent(PREFS_CONNECTION_TYPE_CHANGED_EVENT)
                when (newValue) {
                    bluetooth -> {
                        p1?.isVisible = true
                        p2?.isVisible = false
                        p3?.isVisible = false

                    }
                    "wifi" -> {
                        p1?.isVisible = false
                        p2?.isVisible = true
                        p3?.isVisible = false
                    }

                    "usb" -> {
                        p1?.isVisible = false
                        p2?.isVisible = false
                        p3?.isVisible = true
                    }
                    else -> {
                        p1?.isVisible = false
                        p2?.isVisible = true
                    }
                }
                true
            }
    }

    private fun openPreferenceDialogFor(preferenceKey: String) {
        when (preferenceKey) {
            PREF_GAUGE_RECORDINGS -> TripsPreferenceDialog().show(parentFragmentManager, null)

            PREF_GAUGE_DISPLAYED_PARAMETERS_IDS -> {
                showMultiSelectListDialog(
                    preferenceKey = PREF_GAUGE_DIALOG,
                    targetFragment = this
                ) {
                    navigateToScreen(R.id.navigation_gauge)
                }
            }

            PREF_GRAPH_DISPLAYED_PARAMETERS_IDS -> {
                showMultiSelectListDialog(
                    preferenceKey = PREF_GRAPH_DIALOG,
                    targetFragment = this
                ) {
                    navigateToScreen(R.id.navigation_graph)
                }
            }

            PREF_GIULIA_DISPLAYED_PARAMETERS_IDS -> {
                showMultiSelectListDialog(
                    preferenceKey = "pref.giulia.pids.selected",
                    targetFragment = this
                ) {
                    navigateToScreen(R.id.navigation_giulia)
                }
            }
        }
    }
}