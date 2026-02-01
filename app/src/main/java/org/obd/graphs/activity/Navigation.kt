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

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.dataLogger
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
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.gauge.gaugeVirtualScreen
import org.obd.graphs.ui.giulia.giuliaVirtualScreen


const val NAVIGATION_BUTTONS_VISIBILITY_CHANGED = "navigation.buttons.changes.event"

fun navigateToPreferencesScreen(navigateToPrefKey: String) {
    (getContext() as MainActivity).navController {
        it.navigate(R.id.nav_preferences, bundleOf(PREFERENCE_SCREEN_KEY to navigateToPrefKey))
    }
}

fun navigateToScreen(id: Int) {
    try {
        (getContext() as MainActivity).navController {
            it.navigate(id, null)
        }
    } catch (e: IllegalArgumentException) {
        Log.e(LOG_TAG, "Most probably the $id is not the navigation id.", e)
    }
}

internal fun MainActivity.setupLeftNavigationPanel() {

    leftAppBar { navigationView ->
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
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

                else -> Log.e(LOG_TAG, "Unknown Navigation menu item ${item.itemId}")
            }

            getDrawer().closeDrawer(GravityCompat.START)
            true
        }
    }
}

internal fun MainActivity.setupNavigationBar() {
    navController { navController ->
        bottomAppBar {
            it.setOnApplyWindowInsetsListener(null)
            it.menu.run {
                val mainActivityPreferences = getMainActivityPreferences()
                findItem(R.id.navigation_giulia).isVisible =
                    mainActivityPreferences.showGiuliaView

                findItem(R.id.navigation_gauge).isVisible =
                    mainActivityPreferences.showGaugeView

                findItem(R.id.navigation_dashboard).isVisible =
                    mainActivityPreferences.showDashView

                findItem(R.id.navigation_graph).isVisible =
                    mainActivityPreferences.showGraphView
            }
        }

        NavigationUI.setupWithNavController(findViewById(R.id.bottom_app_bar), navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { _, destination, _ ->

            if (destination.id == R.id.nav_dashboard || destination.id == R.id.nav_graph || destination.id == R.id.nav_giulia) {
                supportActionBar?.setDisplayHomeAsUpEnabled(false)
            } else {
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }


            bottomAppBar {
                it.menu.findItem(R.id.ctx_menu_dtc).isVisible = dataLogger.isDTCEnabled()
                it.menu.findItem(R.id.ctx_menu_android_auto)?.let {
                    if (isAndroidAutoEnabled()) {
                        val spanString = SpannableString(it.title.toString())
                        spanString.setSpan(ForegroundColorSpan(COLOR_PHILIPPINE_GREEN), 0, spanString.length, 0)
                        it.title = spanString
                        it.isVisible = true
                    } else {
                        it.isVisible = false
                    }

                }

                it.menu.findItem(R.id.ctx_menu_views).let {
                    val spanString = SpannableString(it.title.toString()).apply { }
                    spanString.setSpan(ForegroundColorSpan(COLOR_CARDINAL), 0, spanString.length, 0)
                    it.title = spanString
                }

                when (destination.label.toString()) {
                    resources.getString(R.string.navigation_title_graph) -> {
                        it.menu.findItem(R.id.ctx_menu_view_custom_action_1).isVisible = true
                        it.menu.findItem(R.id.ctx_menu_view_custom_action_1).title =
                            resources.getString(R.string.pref_graph_trips_selected)

                        it.menu.findItem(R.id.ctx_menu_submenu_filters).isVisible = true

                        val spanString =
                            SpannableString("${resources.getString(R.string.pref_graph_view_filters)} (${getGraphFilterSource()})")
                        spanString.setSpan(ForegroundColorSpan(COLOR_PHILIPPINE_GREEN), 0, spanString.length, 0)
                        it.menu.findItem(R.id.ctx_menu_submenu_filters).title = spanString
                    }

                    resources.getString(R.string.navigation_title_giulia) -> {
                        it.menu.findItem(R.id.ctx_menu_view_custom_action_1).title =
                            resources.getString(R.string.pref_giulia_apply_graph_filter)
                        it.menu.findItem(R.id.ctx_menu_view_custom_action_1).isVisible = true
                    }

                    else -> {
                        it.menu.findItem(R.id.ctx_menu_view_custom_action_1).isVisible = false
                        it.menu.findItem(R.id.ctx_menu_submenu_filters).isVisible = false

                    }
                }
            }
        }
    }
}



internal fun MainActivity.setupNavigationBarButtons() {
    bottomAppBar {
        it.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.navigation_giulia -> navigateToScreen(R.id.nav_giulia)
                R.id.navigation_graph -> navigateToScreen(R.id.nav_graph)
                R.id.navigation_gauge -> navigateToScreen(R.id.nav_gauge)
                R.id.navigation_dashboard -> navigateToScreen(R.id.nav_dashboard)
                R.id.navigation_preferences -> navigateToScreen(R.id.nav_preferences)

                R.id.ctx_menu_trip_info_view -> navigateToScreen(R.id.nav_trip_info)
                R.id.ctx_menu_performance_view -> navigateToScreen(R.id.nav_performance)
                R.id.ctx_menu_drag_racing_view -> navigateToScreen(R.id.nav_drag_racing)

                R.id.ctx_menu_vehicle_properties -> navigateToPreferencesScreen("pref.vehicle.properties")
                R.id.ctx_menu_about -> navigateToPreferencesScreen("pref.about")
                R.id.ctx_menu_pids_to_query -> navigateToPreferencesScreen("pref.registry")
                R.id.ctx_menu_view_profiles -> navigateToPreferencesScreen("pref.profiles")
                R.id.ctx_menu_dtc -> navigateToPreferencesScreen("pref.dtc")
                R.id.ctx_menu_android_auto -> navigateToPreferencesScreen("pref.aa")

                R.id.ctx_menu_submenu_filters_filter_1 -> applyGraphViewFilter(1)
                R.id.ctx_menu_submenu_filters_filter_2 -> applyGraphViewFilter(2)
                R.id.ctx_menu_submenu_filters_filter_3 -> applyGraphViewFilter(3)
                R.id.ctx_menu_submenu_filters_filter_4 -> applyGraphViewFilter(4)
                R.id.ctx_menu_submenu_filters_filter_5 -> applyGraphViewFilter(5)
                R.id.ctx_menu_submenu_filters_filter_6 -> applyGraphViewFilter(6)
                R.id.ctx_menu_submenu_filters_filter_7 -> applyGraphViewFilter(7)

                R.id.ctx_menu_view_custom_action_1 -> {
                    when (getCurrentScreenId()) {
                        R.id.nav_graph -> {
                            navigateToPreferencesScreen(PREF_GAUGE_TRIPS)
                        }

                        R.id.nav_giulia -> {
                            tripVirtualScreenManager.updateReservedVirtualScreen(
                                Prefs.getStringSet(giuliaVirtualScreen.getVirtualScreenPrefKey()).toList()
                            )
                            tripVirtualScreenManager.updateScreenId(RESERVED_SCREEN_ID)
                            navigateToScreen(R.id.nav_graph)
                        }

                        else -> {}
                    }
                }

                R.id.ctx_menu_pids_to_display -> {

                    val screenId = when (getCurrentScreenId()) {
                        R.id.nav_gauge -> PREFERENCE_SCREEN_KEY_GAUGE
                        R.id.nav_graph -> PREFERENCE_SCREEN_KEY_GRAPH
                        R.id.nav_giulia -> PREFERENCE_SCREEN_KEY_GIULIA
                        R.id.nav_dashboard -> PREFERENCE_SCREEN_KEY_DASH
                        else -> null
                    }

                    Log.e(LOG_TAG, "current screen $screenId  ${getCurrentScreenId()}")
                    screenId?.apply {
                        navigateToPreferencesScreen(this)
                    }
                }

                R.id.ctx_menu_view_configuration -> {
                    navigateToPreferencesScreen(
                        when (getCurrentScreenId()) {
                            R.id.nav_giulia -> "pref.giulia"
                            R.id.navigation_gauge -> "pref.gauge"
                            R.id.nav_graph -> "pref.graph"
                            R.id.nav_dashboard -> "pref.dashboard"
                            else -> "pref.root"
                        }
                    )
                }
            }
            true
        }
    }
}

private fun MainActivity.applyGraphViewFilter(screenId: Int) {
    val propertyId: String? = when (getGraphFilterSource()) {
        "Giulia" -> giuliaVirtualScreen.getVirtualScreenPrefKey("$screenId")
        "Gauge" -> gaugeVirtualScreen.getVirtualScreenPrefKey("$screenId")
        "AA" -> {
            if (isAndroidAutoEnabled()) {
                "pref.aa.pids.profile_$screenId"
            } else {
                null
            }
        }

        else -> giuliaVirtualScreen.getVirtualScreenPrefKey("$screenId")
    }

    Log.i(LOG_TAG, "Applying graph view filter for property.id=$propertyId")

    propertyId?.let {
        val filter = Prefs.getStringSet(propertyId).toList()
        Log.i(LOG_TAG, "Applying graph view filter=$filter")
        tripVirtualScreenManager.updateReservedVirtualScreen(filter)
        tripVirtualScreenManager.updateScreenId()

        navigateToScreen(R.id.nav_graph)
    }
}

private fun getGraphFilterSource() = Prefs.getS("pref.graph.filter.source", "Giulia")

private fun MainActivity.isAndroidAutoEnabled() = resources.getBoolean(R.bool.MODULE_ANDROID_AUTO_ENABLED)


private fun MainActivity.getCurrentScreenId(): Int {
    val navHostFragment = supportFragmentManager
        .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
    return navHostFragment?.navController?.currentDestination?.id ?: -1
}
