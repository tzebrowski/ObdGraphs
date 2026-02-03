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
import androidx.navigation.ui.NavigationUI
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.getContext
import org.obd.graphs.preferences.PREFERENCE_SCREEN_KEY
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.isEnabled
import org.obd.graphs.preferences.updateInt
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN

internal const val NAVIGATION_BUTTONS_VISIBILITY_CHANGED = "navigation.buttons.changes.event"
internal const val PREF_NAVIGATION_LAST_VISITED_SCREEN = "pref.navigation.last_visited.screen"
internal const val PREF_NAVIGATION_LAST_VISITED_SCREEN_ENABLED = "pref.views.navigation.navigate_last_visited_view"

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
        Prefs.updateInt(PREF_NAVIGATION_LAST_VISITED_SCREEN, id)
    } catch (e: IllegalArgumentException) {
        Log.e(LOG_TAG, "Most probably the $id is not the navigation id.", e)
    }
}

internal fun MainActivity.setupBottomBarNavigation() {
    bottomAppBar {
        it.setOnMenuItemClickListener { item ->
            NavigationRouter.navigate(this, item.itemId)
            true
        }
    }
}

internal fun MainActivity.setupNavigationViewNavigation() {
    leftAppBar { navigationView ->
        navigationView.setNavigationItemSelectedListener { item ->
            if (NavigationRouter.navigate(this, item.itemId)) {
                getDrawer().closeDrawer(GravityCompat.START)
            } else {
                Log.e(LOG_TAG, "Unknown Navigation menu item ${item.itemId}")
            }
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

            bottomAppBar {
                it.menu.findItem(R.id.ctx_menu_dtc).isVisible = DataLoggerRepository.isDTCEnabled() ?: false
                it.menu.findItem(R.id.ctx_menu_android_auto)?.let {
                    if (NavigationRouter.isAndroidAutoEnabled(this)) {
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
                            SpannableString(
                                "${resources.getString(R.string.pref_graph_view_filters)} (${NavigationRouter.getGraphFilterSource()})",
                            )
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

internal fun navigateToLastVisitedScreen() {
    val lastVisitedScreen = Prefs.getInt(PREF_NAVIGATION_LAST_VISITED_SCREEN, -1)
    if (lastVisitedScreen != -1 && Prefs.isEnabled(PREF_NAVIGATION_LAST_VISITED_SCREEN_ENABLED)) {
        Log.i(LOG_TAG, "Loading last visited view: $lastVisitedScreen")
        navigateToScreen(lastVisitedScreen)
    }
}
