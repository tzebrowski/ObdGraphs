/**
 * Copyright 2019-2023, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
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
import org.obd.graphs.preferences.dtc.DiagnosticTroubleCodeListPreferences
import org.obd.graphs.preferences.dtc.DiagnosticTroubleCodePreferenceDialogFragment
import org.obd.graphs.preferences.metadata.VehicleMetadataListPreferences
import org.obd.graphs.preferences.metadata.VehicleMetadataPreferenceDialogFragment
import org.obd.graphs.preferences.pid.PIDsListPreferenceDialogFragment
import org.obd.graphs.preferences.pid.PIDsListPreferences
import org.obd.graphs.preferences.trips.TripsListPreferences
import org.obd.graphs.preferences.trips.TripsPreferenceDialogFragment
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.onDoubleClickListener
import org.obd.graphs.ui.gauge.gaugeVirtualScreen
import org.obd.graphs.ui.giulia.giuliaVirtualScreen
import org.obd.graphs.ui.graph.graphVirtualScreen

const val PREFERENCE_SCREEN_KEY = "preferences.rootKey"
const val PREFS_CONNECTION_TYPE_CHANGED_EVENT = "prefs.connection_type.changed.event"

const val PREF_GAUGE_RECORDINGS = "pref.gauge.recordings"
const val PREF_DASH_DISPLAYED_PARAMETERS_IDS = "pref.dash.displayed_parameter_ids"
const val PREF_GAUGE_DISPLAYED_PARAMETERS_IDS = "pref.gauge.displayed_parameter_ids"
const val PREF_GRAPH_DISPLAYED_PARAMETERS_IDS = "pref.graph.displayed_parameter_ids"
const val PREF_GIULIA_DISPLAYED_PARAMETERS_IDS = "pref.giulia.displayed_parameter_ids"
const val PREFERENCE_CONNECTION_TYPE = "pref.adapter.connection.type"

private const val LOG_KEY = "Prefs"

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

            is PIDsListPreferences -> {
                when (preference.source) {
                    "dash" -> {
                        openPIDsDialog("pref.dash.pids.selected","dashboard")
                            { navigateToScreen(R.id.navigation_dashboard) }
                    }
                    "graph" -> {
                        openPIDsDialog(graphVirtualScreen.getVirtualScreenPrefKey(),preference.source)
                        { navigateToScreen(R.id.navigation_graph) }
                    }

                    "giulia" -> {
                        openPIDsDialog(giuliaVirtualScreen.getVirtualScreenPrefKey(),preference.source)
                        { navigateToScreen(R.id.navigation_giulia) }
                    }

                    "gauge" -> {
                        openPIDsDialog(gaugeVirtualScreen.getVirtualScreenPrefKey(),preference.source)
                        { navigateToScreen(R.id.navigation_gauge) }
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

            PREF_DASH_DISPLAYED_PARAMETERS_IDS ->
                openPIDsDialog("pref.dash.pids.selected","dashboard")
                { navigateToScreen(R.id.navigation_dashboard) }

            PREF_GAUGE_DISPLAYED_PARAMETERS_IDS ->
                openPIDsDialog(gaugeVirtualScreen.getVirtualScreenPrefKey(),"gauge")
                { navigateToScreen(R.id.navigation_gauge) }

            PREF_GIULIA_DISPLAYED_PARAMETERS_IDS ->
                openPIDsDialog(giuliaVirtualScreen.getVirtualScreenPrefKey(),"giulia")
                { navigateToScreen(R.id.navigation_giulia) }

            PREF_GRAPH_DISPLAYED_PARAMETERS_IDS ->
                openPIDsDialog(graphVirtualScreen.getVirtualScreenPrefKey(),"graph")
                { navigateToScreen(R.id.navigation_graph) }
        }
    }

    private fun openPIDsDialog(key: String, source: String, onDialogCloseListener: (() -> Unit) = {}) {
        val detailsViewVisible = source == "low" || source == "high"
        PIDsListPreferenceDialogFragment(key = key, source = source,
            detailsViewEnabled = detailsViewVisible,
            onDialogCloseListener = onDialogCloseListener)
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