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
package org.obd.graphs.activity

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.getContext
import org.obd.graphs.preferences.*
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN


fun navigateToPreferencesScreen(prefKey: String) {
    (getContext() as MainActivity).navController {
        it.navigate(R.id.navigation_preferences, bundleOf(PREFERENCE_SCREEN_KEY to prefKey))
    }
}

fun navigateToScreen(id: Int) {
    (getContext() as MainActivity).navController {
        it.navigate(id, null)
    }
}

internal fun MainActivity.setupLeftNavigationPanel() {
    val drawerLayout: DrawerLayout = getDrawer()
    val actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close)
    drawerLayout.addDrawerListener(actionBarDrawerToggle)
    actionBarDrawerToggle.syncState()
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    leftAppBar { navigationView  ->
        navigationView .setNavigationItemSelectedListener { item ->
            when (item.itemId){
                R.id.navigation_android_auto ->{
                    navigateToPreferencesScreen("pref.aa")
                }
                else -> {
                    navigateToScreen(item.itemId)
                }
            }

            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }
}

internal fun MainActivity.setupNavigationBar() {
    navController { navController ->
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_gauge,
                R.id.navigation_graph,
                R.id.navigation_giulia,
                R.id.navigation_dashboard,
                R.id.navigation_preferences,
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        findViewById<BottomNavigationView>(R.id.bottom_nav_view).let {
            it.setupWithNavController(navController)
            it.selectedItemId = R.id.navigation_gauge
        }

        val mainActivityPreferences = getMainActivityPreferences()
        findViewById<BottomNavigationView>(R.id.bottom_nav_view).menu.run {
            findItem(R.id.navigation_giulia).isVisible =
                mainActivityPreferences.showGiuliaView

            findItem(R.id.navigation_gauge).isVisible =
                mainActivityPreferences.showGaugeView

            findItem(R.id.navigation_dashboard).isVisible =
                mainActivityPreferences.showDashView

            findItem(R.id.navigation_graph).isVisible =
                mainActivityPreferences.showGraphView
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomAppBar {
                it.menu.findItem(R.id.ctx_menu_dtc).isVisible = dataLogger.isDTCEnabled()
                val aaMenuItem = it.menu.findItem(R.id.ctx_menu_android_auto)
                if (resources.getBoolean(R.bool.MODULE_ANDROID_AUTO_ENABLED)){
                    val spanString = SpannableString(aaMenuItem.title.toString())
                    spanString.setSpan(ForegroundColorSpan(COLOR_PHILIPPINE_GREEN), 0, spanString.length, 0)
                    aaMenuItem.title = spanString
                    aaMenuItem.isVisible = true
                } else {
                    aaMenuItem.isVisible = false
                }

                when (destination.label.toString()) {
                    resources.getString(R.string.navigation_title_graph) -> {
                        it.menu.findItem(R.id.ctx_menu_view_custom_action_1).isVisible = true
                        it.menu.findItem(R.id.ctx_menu_view_custom_action_1).title =
                            resources.getString(R.string.pref_graph_trips_selected)
                    }
                    else -> {
                        it.menu.findItem(R.id.ctx_menu_view_custom_action_1).isVisible = false
                    }
                }
            }
        }
    }
}

internal fun MainActivity.setupNavigationBarButtons() {
    floatingActionButton {
        it.setOnClickListener {
            Log.i(LOG_TAG, "Start data logging")
            dataLogger.start()
        }
    }
    bottomAppBar {
        it.setOnMenuItemClickListener { item ->
            when (item.itemId) {

                R.id.ctx_menu_view_custom_action_1 -> {
                    val screenId = when (getCurrentScreenId()) {
                        R.id.navigation_graph -> PREF_GAUGE_RECORDINGS
                        else -> null
                    }
                    screenId?.apply {
                        navigateToPreferencesScreen(this)
                    }
                }

                R.id.ctx_menu_pids_to_display -> {
                    val screenId = when (getCurrentScreenId()) {
                        R.id.navigation_gauge -> PREF_GAUGE_DISPLAYED_PARAMETERS_IDS
                        R.id.navigation_graph -> PREF_GRAPH_DISPLAYED_PARAMETERS_IDS
                        R.id.navigation_giulia -> PREF_GIULIA_DISPLAYED_PARAMETERS_IDS

                        else -> null
                    }
                    screenId?.apply {
                        navigateToPreferencesScreen(this)
                    }
                }

                R.id.ctx_menu_vehicle_properties -> {
                    navigateToPreferencesScreen("pref.vehicle.properties")
                }

                R.id.ctx_menu_about -> {
                    navigateToPreferencesScreen("pref.about")
                }

                R.id.ctx_menu_pids_to_query -> {
                    navigateToPreferencesScreen("pref.registry")
                }

                R.id.ctx_menu_view_profiles -> {
                    navigateToPreferencesScreen("pref.profiles")
                }

                R.id.ctx_menu_dtc -> {
                    navigateToPreferencesScreen("pref.dtc")
                }

                R.id.ctx_menu_android_auto -> {
                    navigateToPreferencesScreen("pref.aa")
                }

                R.id.ctx_menu_view_configuration -> {
                    navigateToPreferencesScreen(
                        when (getCurrentScreenId()) {
                            R.id.navigation_giulia -> "pref.giulia"
                            R.id.navigation_gauge -> "pref.gauge"
                            R.id.navigation_graph -> "pref.graph"
                            R.id.navigation_dashboard -> "pref.dashboard"
                            else -> "pref.root"
                        }
                    )
                }
            }
            true
        }
    }
}

private fun MainActivity.getCurrentScreenId(): Int {
    val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_nav_view)
    val selectedItemId: Int = bottomNavigationView.selectedItemId
    val currentView: MenuItem =
        bottomNavigationView.menu.findItem(selectedItemId)
    return currentView.itemId
}