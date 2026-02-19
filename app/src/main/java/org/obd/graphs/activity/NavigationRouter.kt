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
package org.obd.graphs.activity

import android.content.Context
import android.util.Log
import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import org.obd.graphs.R
import org.obd.graphs.bl.trip.RESERVED_SCREEN_ID
import org.obd.graphs.bl.trip.tripVirtualScreenManager
import org.obd.graphs.getContext
import org.obd.graphs.preferences.PREFERENCE_SCREEN_KEY
import org.obd.graphs.preferences.PREFERENCE_SCREEN_KEY_DASH
import org.obd.graphs.preferences.PREFERENCE_SCREEN_KEY_GAUGE
import org.obd.graphs.preferences.PREFERENCE_SCREEN_KEY_GIULIA
import org.obd.graphs.preferences.PREFERENCE_SCREEN_KEY_GRAPH
import org.obd.graphs.preferences.PREF_GAUGE_TRIPS
import org.obd.graphs.preferences.PREF_LOGS
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import org.obd.graphs.preferences.getStringSet
import org.obd.graphs.preferences.updateInt
import org.obd.graphs.ui.gauge.gaugeVirtualScreenPreferences
import org.obd.graphs.ui.giulia.giuliaVirtualScreenPreferences

 internal object NavigationRouter {
    fun navigate(
        activity: MainActivity,
        itemId: Int,
    ): Boolean =
        when (itemId) {
            R.id.navigation_preferences_alerts -> navigateToPreferencesScreen("pref.alerts.settings")
            R.id.navigation_performance_prefs -> navigateToPreferencesScreen("pref.title_performance_screen")
            R.id.navigation_performance_pids_list_prefs -> navigateToPreferencesScreen("pref.performance.displayed_parameter_ids")

            R.id.navigation_trip_info_prefs -> navigateToPreferencesScreen("pref.title_trip_info")
            R.id.navigation_trip_info_pids_list_prefs -> navigateToPreferencesScreen("pref.trip_info.displayed_parameter_ids")
            R.id.navigation_drag_racing_prefs -> navigateToPreferencesScreen("pref.title_drag_racing")
            R.id.navigation_graph_prefs -> navigateToPreferencesScreen("pref.graph")
            R.id.navigation_dashboard_prefs -> navigateToPreferencesScreen("pref.dashboard")
            R.id.navigation_giulia_prefs -> navigateToPreferencesScreen("pref.giulia")
            R.id.navigation_gauge_prefs -> navigateToPreferencesScreen("pref.gauge")
            R.id.ctx_menu_android_auto -> navigateToPreferencesScreen("pref.aa")
            R.id.ctx_menu_vehicle_properties -> navigateToPreferencesScreen("pref.vehicle.properties")
            R.id.ctx_menu_about -> navigateToPreferencesScreen("pref.about")
            R.id.ctx_menu_view_profiles -> navigateToPreferencesScreen("pref.profiles")
            R.id.ctx_menu_dtc -> navigateToPreferencesScreen("pref.dtc")

            R.id.navigation_android_giulia_auto_pids -> navigateToPreferencesScreen("pref.aa.displayed.pids")
            R.id.navigation_android_gauge_auto_pids -> navigateToPreferencesScreen("pref.aa.gauge.displayed.pids")

            R.id.navigation_android_giulia_auto_font_size -> navigateToPreferencesScreen("pref.aa.screen.font_size.category")
            R.id.navigation_android_gauge_auto_font_size -> navigateToPreferencesScreen("pref.aa.gauge.screen.font_size.category")

            R.id.navigation_gauge_pids -> navigateToPreferencesScreen(PREFERENCE_SCREEN_KEY_GAUGE)
            R.id.navigation_graph_pids -> navigateToPreferencesScreen(PREFERENCE_SCREEN_KEY_GRAPH)
            R.id.navigation_giulia_pids -> navigateToPreferencesScreen(PREFERENCE_SCREEN_KEY_GIULIA)
            R.id.navigation_giulia_font_size -> navigateToPreferencesScreen("pref.giulia.screen.font_size.category")
            R.id.navigation_giulia_number_of_columns -> navigateToPreferencesScreen("pref.aa.number_of_items_in_column.category")

            R.id.navigation_dashboard_pids -> navigateToPreferencesScreen(PREFERENCE_SCREEN_KEY_DASH)
            R.id.navigation_graph_tripe -> navigateToPreferencesScreen(PREF_GAUGE_TRIPS)
            R.id.ctx_menu_pids_to_query -> navigateToPreferencesScreen("pref.registry")
            R.id.nav_preferences -> navigateToPreferencesScreen("pref.root")
            R.id.navigation_preferences_adapter -> navigateToPreferencesScreen("pref.adapter.connection")
            R.id.navigation_adapter_dri -> navigateToPreferencesScreen("pref.init")
            R.id.navigation_trip_logs -> navigateToPreferencesScreen(PREF_LOGS)

            R.id.navigation_giulia -> navigateToScreen(R.id.nav_giulia)
            R.id.navigation_graph -> navigateToScreen(R.id.nav_graph)
            R.id.navigation_gauge -> navigateToScreen(R.id.nav_gauge)
            R.id.navigation_dashboard -> navigateToScreen(R.id.nav_dashboard)
            R.id.navigation_preferences -> navigateToScreen(R.id.nav_preferences)
            R.id.navigation_drag_racing -> navigateToScreen(R.id.nav_drag_racing)
            R.id.navigation_trip_info -> navigateToScreen(R.id.nav_trip_info)
            R.id.navigation_performance -> navigateToScreen(R.id.nav_performance)

            R.id.ctx_menu_trip_info_view -> navigateToScreen(R.id.nav_trip_info)
            R.id.ctx_menu_performance_view -> navigateToScreen(R.id.nav_performance)
            R.id.ctx_menu_drag_racing_view -> navigateToScreen(R.id.nav_drag_racing)

            R.id.ctx_menu_submenu_filters_filter_1 -> applyGraphViewFilter(1)
            R.id.ctx_menu_submenu_filters_filter_2 -> applyGraphViewFilter(2)
            R.id.ctx_menu_submenu_filters_filter_3 -> applyGraphViewFilter(3)
            R.id.ctx_menu_submenu_filters_filter_4 -> applyGraphViewFilter(4)
            R.id.ctx_menu_submenu_filters_filter_5 -> applyGraphViewFilter(5)
            R.id.ctx_menu_submenu_filters_filter_6 -> applyGraphViewFilter(6)
            R.id.ctx_menu_submenu_filters_filter_7 -> applyGraphViewFilter(7)

            R.id.ctx_menu_view_custom_action_1 ->
                when (getCurrentScreenId(activity)) {
                    R.id.nav_graph -> {
                        navigateToPreferencesScreen(PREF_GAUGE_TRIPS)
                    }

                    R.id.nav_giulia -> {
                        tripVirtualScreenManager.updateReservedVirtualScreen(
                            Prefs.getStringSet(giuliaVirtualScreenPreferences.getVirtualScreenPrefKey()).toList(),
                        )
                        tripVirtualScreenManager.updateScreenId(RESERVED_SCREEN_ID)
                        navigateToScreen(R.id.nav_graph)
                    }
                    else -> false
                }

            R.id.ctx_menu_pids_to_display -> {
                val screenId =
                    when (getCurrentScreenId(activity)) {
                        R.id.nav_gauge -> PREFERENCE_SCREEN_KEY_GAUGE
                        R.id.nav_graph -> PREFERENCE_SCREEN_KEY_GRAPH
                        R.id.nav_giulia -> PREFERENCE_SCREEN_KEY_GIULIA
                        R.id.nav_dashboard -> PREFERENCE_SCREEN_KEY_DASH
                        else -> null
                    }

                Log.d(LOG_TAG, "Jumping to preference screen for current screen $screenId  ${getCurrentScreenId(activity)}")
                screenId?.apply {
                    navigateToPreferencesScreen(this)
                    return true
                }
                false
            }

            R.id.ctx_menu_view_configuration -> {
                navigateToPreferencesScreen(
                    when (getCurrentScreenId(activity)) {
                        R.id.nav_giulia -> "pref.giulia"
                        R.id.nav_gauge -> "pref.gauge"
                        R.id.nav_graph -> "pref.graph"
                        R.id.nav_dashboard -> "pref.dashboard"
                        else -> "pref.root"
                    },
                )
            }

            else -> false
        }

    fun navigateToPreferencesScreen(navigateToPrefKey: String): Boolean {
        (getContext() as MainActivity).navController {
            it.navigate(R.id.nav_preferences, bundleOf(PREFERENCE_SCREEN_KEY to navigateToPrefKey))
        }
        return true
    }

    fun navigateToScreen(id: Int): Boolean {
        try {
            (getContext() as MainActivity).navController {
                it.navigate(id, null)
            }
            Prefs.updateInt(PREF_NAVIGATION_LAST_VISITED_SCREEN, id)
        } catch (e: IllegalArgumentException) {
            Log.e(LOG_TAG, "Most probably the $id is not the navigation id.", e)
        }
        return true
    }

    internal fun getGraphFilterSource() = Prefs.getS("pref.graph.filter.source", "Giulia")

    internal fun isAndroidAutoEnabled(context: Context) = context.resources.getBoolean(R.bool.MODULE_ANDROID_AUTO_ENABLED)


    private fun applyGraphViewFilter(screenId: Int): Boolean {
        val propertyId: String? =
            when (getGraphFilterSource()) {
                "Giulia" -> giuliaVirtualScreenPreferences.getVirtualScreenPrefKey("$screenId")
                "Gauge" -> gaugeVirtualScreenPreferences.getVirtualScreenPrefKey("$screenId")
                "AA" -> {
                    if (isAndroidAutoEnabled(getContext()!!)) {
                        "pref.aa.pids.profile_$screenId"
                    } else {
                        null
                    }
                }

                else -> giuliaVirtualScreenPreferences.getVirtualScreenPrefKey("$screenId")
            }

        Log.i(LOG_TAG, "Applying graph view filter for property.id=$propertyId")

        propertyId?.let {
            val filter = Prefs.getStringSet(propertyId).toList()
            Log.i(LOG_TAG, "Applying graph view filter=$filter")
            tripVirtualScreenManager.updateReservedVirtualScreen(filter)
            tripVirtualScreenManager.updateScreenId()

            navigateToScreen(R.id.nav_graph)
            return true
        }
        return false
    }


    private fun getCurrentScreenId(activity: MainActivity): Int {
        val navHostFragment =
            activity.supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        return navHostFragment?.navController?.currentDestination?.id ?: -1
    }
}
