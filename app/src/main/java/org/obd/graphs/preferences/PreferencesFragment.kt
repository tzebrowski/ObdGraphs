 /**
 * Copyright 2019-2025, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import org.obd.graphs.activity.*
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.trip.tripVirtualScreenManager
import org.obd.graphs.preferences.dtc.DiagnosticTroubleCodeListPreferences
import org.obd.graphs.preferences.dtc.DiagnosticTroubleCodePreferenceDialogFragment
import org.obd.graphs.preferences.metadata.VehicleMetadataListPreferences
import org.obd.graphs.preferences.metadata.VehicleMetadataPreferenceDialogFragment
import org.obd.graphs.preferences.pid.PidDefinitionPreferenceDialogFragment
import org.obd.graphs.preferences.pid.PidDefinitionListPreferences
import org.obd.graphs.preferences.trips.TripsListPreferences
import org.obd.graphs.preferences.trips.TripsPreferenceDialogFragment
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.onDoubleClickListener
import org.obd.graphs.ui.gauge.gaugeVirtualScreen
import org.obd.graphs.ui.giulia.giuliaVirtualScreen

const val PREFERENCE_SCREEN_KEY = "preferences.rootKey"
const val PREFS_CONNECTION_TYPE_CHANGED_EVENT = "prefs.connection_type.changed.event"

const val PREF_GAUGE_RECORDINGS = "pref.gauge.recordings"
const val PREFERENCE_CONNECTION_TYPE = "pref.adapter.connection.type"
private const val LOG_KEY = "Prefs"

const val PREFERENCE_SCREEN_KEY_TRIP_INFO = "pref.trip_info.displayed_parameter_ids"
const val PREFERENCE_SCREEN_KEY_PERFORMANCE = "pref.performance.displayed_parameter_ids"
const val PREFERENCE_SCREEN_KEY_DASH = "pref.dash.displayed_parameter_ids"
const val PREFERENCE_SCREEN_KEY_GAUGE = "pref.gauge.displayed_parameter_ids"
const val PREFERENCE_SCREEN_KEY_GRAPH = "pref.graph.displayed_parameter_ids"
const val PREFERENCE_SCREEN_KEY_GIULIA = "pref.giulia.displayed_parameter_ids"
const val PREFERENCE_SCREEN_SOURCE_TRIP_INFO = "trip_info"
const val PREFERENCE_SCREEN_SOURCE_PERFORMANCE = "performance"
private const val PREFERENCE_SCREEN_SOURCE_GIULIA = "giulia"
private const val PREFERENCE_SCREEN_SOURCE_GAUGE = "gauge"
private const val PREFERENCE_SCREEN_SOURCE_GRAPH = "graph"
private const val NAVIGATE_TO_PREF_KEY = "pref.aa"

private const val PREFERENCE_SCREEN_SOURCE_DASHBOARD = "dashboard"

class PreferencesFragment : PreferenceFragmentCompat() {

    private val onSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            Log.v(LOG_KEY, "Preference $key changed")
        }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference) {

            is TripsListPreferences -> {
                TripsPreferenceDialogFragment().show(parentFragmentManager, null)
            }

            is VehicleMetadataListPreferences -> {
                VehicleMetadataPreferenceDialogFragment().show(parentFragmentManager, null)
            }

            is PidDefinitionListPreferences -> {
                openPreferenceDialogFor(preference.source)
                when (preference.source) {
                    "dash" -> {
                        openPIDsDialog("pref.dash.pids.selected", PREFERENCE_SCREEN_SOURCE_DASHBOARD)
                        { navigateToScreen(R.id.navigation_dashboard) }
                    }

                    PREFERENCE_SCREEN_SOURCE_GRAPH -> {
                        openPIDsDialog(tripVirtualScreenManager.getVirtualScreenPrefKey(), preference.source)
                        { navigateToScreen(R.id.navigation_graph) }
                    }

                    PREFERENCE_SCREEN_SOURCE_GIULIA -> {
                        openPIDsDialog(giuliaVirtualScreen.getVirtualScreenPrefKey(), preference.source)
                        { navigateToScreen(R.id.navigation_giulia) }
                    }

                    PREFERENCE_SCREEN_SOURCE_GAUGE -> {
                        openPIDsDialog(gaugeVirtualScreen.getVirtualScreenPrefKey(), preference.source)
                        { navigateToScreen(R.id.navigation_gauge) }
                    }

                    PREFERENCE_SCREEN_SOURCE_TRIP_INFO -> {
                        openPIDsDialog(preference.key, preference.source)
                        { navigateToPreferencesScreen(NAVIGATE_TO_PREF_KEY) }
                    }

                    PREFERENCE_SCREEN_SOURCE_PERFORMANCE -> {
                        openPIDsDialog(preference.key, preference.source)
                        { navigateToPreferencesScreen(NAVIGATE_TO_PREF_KEY) }
                    }


                    else -> {
                        openPIDsDialog(preference.key, preference.source)
                    }
                }
            }

            is DiagnosticTroubleCodeListPreferences -> {
                DiagnosticTroubleCodePreferenceDialogFragment().show(parentFragmentManager, null)
            }

            else -> {
                super.onDisplayPreferenceDialog(preference)
            }
        }
    }

    override fun onNavigateToScreen(preferenceScreen: PreferenceScreen) {
        sendBroadcastEvent(RESET_TOOLBAR_ANIMATION)
        // add to navigation chain
        navigateToPreferencesScreen(preferenceScreen.key)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (arguments == null) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        } else {
            requireArguments().getString(PREFERENCE_SCREEN_KEY)?.let {
                Log.d(LOG_KEY, "Loading Pref Screen for key=$it")

                setPreferencesFromResource(
                    R.xml.preferences,
                    it
                )
                openPreferenceDialogFor(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(
            onSharedPreferenceChangeListener
        )
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(
            onSharedPreferenceChangeListener
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        sendBroadcastEvent(RESET_TOOLBAR_ANIMATION)
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
            PREF_GAUGE_RECORDINGS -> TripsPreferenceDialogFragment().show(parentFragmentManager, null)

            PREFERENCE_SCREEN_KEY_TRIP_INFO ->
                openPIDsDialog("pref.aa.trip_info.pids.selected", PREFERENCE_SCREEN_SOURCE_TRIP_INFO)
                { navigateToPreferencesScreen(NAVIGATE_TO_PREF_KEY) }

            PREFERENCE_SCREEN_KEY_PERFORMANCE ->
                openPIDsDialog("pref.aa.performance.pids.selected", PREFERENCE_SCREEN_SOURCE_PERFORMANCE)
                { navigateToPreferencesScreen(NAVIGATE_TO_PREF_KEY) }

            PREFERENCE_SCREEN_KEY_DASH ->
                openPIDsDialog("pref.dash.pids.selected", PREFERENCE_SCREEN_SOURCE_DASHBOARD)
                { navigateToScreen(R.id.navigation_dashboard) }

            PREFERENCE_SCREEN_KEY_GAUGE ->
                openPIDsDialog(gaugeVirtualScreen.getVirtualScreenPrefKey(), PREFERENCE_SCREEN_SOURCE_GAUGE)
                { navigateToScreen(R.id.navigation_gauge) }

            PREFERENCE_SCREEN_KEY_GIULIA ->
                openPIDsDialog(giuliaVirtualScreen.getVirtualScreenPrefKey(), PREFERENCE_SCREEN_SOURCE_GIULIA)
                { navigateToScreen(R.id.navigation_giulia) }

            PREFERENCE_SCREEN_KEY_GRAPH ->
                openPIDsDialog(tripVirtualScreenManager.getVirtualScreenPrefKey(), PREFERENCE_SCREEN_SOURCE_GRAPH)
                { navigateToScreen(R.id.navigation_graph) }
        }
    }

    private fun openPIDsDialog(key: String, source: String, onDialogCloseListener: (() -> Unit) = {}) {
        PidDefinitionPreferenceDialogFragment(
            key = key, source = source,
            onDialogCloseListener = onDialogCloseListener
        )
            .show(parentFragmentManager, null)
    }


    private fun registerViewsPreferenceChangeListeners() {
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
            GIULIA_VIEW_ID,
            NOTIFICATION_GIULIA_VIEW_TOGGLE
        )
    }

    private fun registerCheckboxListener(key: String, actionName: String) {
        val preference = findPreference<CheckBoxPreference>(key)
        preference?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                sendBroadcastEvent(actionName)
                true
            }
    }
}
