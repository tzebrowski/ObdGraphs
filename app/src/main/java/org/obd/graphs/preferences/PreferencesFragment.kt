 /**
 * Copyright 2019-2026, Tomasz Żebrowski
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

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.*
import org.obd.graphs.R
import org.obd.graphs.activity.*
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.trip.tripVirtualScreenManager
import org.obd.graphs.preferences.dtc.DiagnosticTroubleCodeListPreferences
import org.obd.graphs.preferences.dtc.DiagnosticTroubleCodePreferenceDialogFragment
import org.obd.graphs.preferences.metadata.VehicleMetadataListPreferences
import org.obd.graphs.preferences.metadata.VehicleMetadataPreferenceDialogFragment
import org.obd.graphs.preferences.pid.PidDefinitionPreferenceDialogFragment
import org.obd.graphs.preferences.pid.PidDefinitionListPreferences
import org.obd.graphs.preferences.trips.TripsListPreferences
import org.obd.graphs.preferences.trips.TripLogListDialogFragment
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.onDoubleClickListener
import org.obd.graphs.ui.gauge.gaugeVirtualScreenPreferences
import org.obd.graphs.ui.giulia.giuliaVirtualScreenPreferences

const val PREFERENCE_SCREEN_KEY = "preferences.rootKey"
const val PREFS_CONNECTION_TYPE_CHANGED_EVENT = "prefs.connection_type.changed.event"

const val PREF_GAUGE_TRIPS = "pref.gauge.recordings"
const val PREF_LOGS = "pref.trip_logs"

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

    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference) {

            is TripsListPreferences -> {
                TripLogListDialogFragment().show(parentFragmentManager, null)
            }

            is VehicleMetadataListPreferences -> {
                VehicleMetadataPreferenceDialogFragment().show(parentFragmentManager, null)
            }

            is PidDefinitionListPreferences -> {
                openPreferenceDialogFor(preference.source)
                when (preference.source) {
                    "dash" -> {
                        openPIDsDialog("pref.dash.pids.selected", PREFERENCE_SCREEN_SOURCE_DASHBOARD)
                        { navigateToScreen(R.id.nav_dashboard) }
                    }

                    PREFERENCE_SCREEN_SOURCE_GRAPH -> {
                        openPIDsDialog(tripVirtualScreenManager.getVirtualScreenPrefKey(), preference.source)
                        { navigateToScreen(R.id.nav_graph) }
                    }

                    PREFERENCE_SCREEN_SOURCE_GIULIA -> {
                        openPIDsDialog(giuliaVirtualScreenPreferences.getVirtualScreenPrefKey(), preference.source)
                        { navigateToScreen(R.id.nav_giulia) }
                    }

                    PREFERENCE_SCREEN_SOURCE_GAUGE -> {
                        openPIDsDialog(gaugeVirtualScreenPreferences.getVirtualScreenPrefKey(), preference.source)
                        { navigateToScreen(R.id.nav_gauge) }
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
        sendBroadcastEvent(TOOLBAR_SHOW)
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        sendBroadcastEvent(TOOLBAR_SHOW)
        val root = super.onCreateView(inflater, container, savedInstanceState)
        registerListeners()
        customizeListView()
        hidePreferences()
        return root
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun customizeListView() {
        listView.setBackgroundColor(Color.LTGRAY)
        listView.setOnTouchListener(onDoubleClickListener(requireContext()))
    }

    private fun hidePreferences() {
        findPreference<PreferenceCategory>("pref.dtc.category")?.isVisible =
            DataLoggerRepository.isDTCEnabled()
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
            PREF_GAUGE_TRIPS -> TripLogListDialogFragment(enableUploadCloudButton = false).show(parentFragmentManager, null)
            PREF_LOGS -> TripLogListDialogFragment(enableDeleteButtons = false).show(parentFragmentManager, null)

            PREFERENCE_SCREEN_KEY_TRIP_INFO ->
                openPIDsDialog("pref.aa.trip_info.pids.selected", PREFERENCE_SCREEN_SOURCE_TRIP_INFO)
                { navigateToScreen(R.id.nav_trip_info) }

            PREFERENCE_SCREEN_KEY_PERFORMANCE ->
                openPIDsDialog("pref.aa.performance.pids.selected", PREFERENCE_SCREEN_SOURCE_PERFORMANCE)
                { navigateToScreen(R.id.nav_performance) }

            PREFERENCE_SCREEN_KEY_DASH ->
                openPIDsDialog("pref.dash.pids.selected", PREFERENCE_SCREEN_SOURCE_DASHBOARD)
                { navigateToScreen(R.id.nav_dashboard) }

            PREFERENCE_SCREEN_KEY_GAUGE ->
                openPIDsDialog(gaugeVirtualScreenPreferences.getVirtualScreenPrefKey(), PREFERENCE_SCREEN_SOURCE_GAUGE)
                { navigateToScreen(R.id.nav_gauge) }

            PREFERENCE_SCREEN_KEY_GIULIA ->
                openPIDsDialog(giuliaVirtualScreenPreferences.getVirtualScreenPrefKey(), PREFERENCE_SCREEN_SOURCE_GIULIA)
                { navigateToScreen(R.id.nav_giulia) }

            PREFERENCE_SCREEN_KEY_GRAPH ->
                openPIDsDialog(tripVirtualScreenManager.getVirtualScreenPrefKey(), PREFERENCE_SCREEN_SOURCE_GRAPH)
                { navigateToScreen(R.id.nav_graph) }
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
