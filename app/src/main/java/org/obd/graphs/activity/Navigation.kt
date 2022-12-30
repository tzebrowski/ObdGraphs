package org.obd.graphs.activity

import android.util.Log
import android.view.MenuItem
import androidx.core.os.bundleOf
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.bl.datalogger.DataLoggerService
import org.obd.graphs.getContext
import org.obd.graphs.preferences.PREFERENCE_SCREEN_KEY
import org.obd.graphs.preferences.PREF_GAUGE_RECORDINGS


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

internal fun MainActivity.setupNavigationBar() {
    navController { navController ->
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_gauge,
                R.id.navigation_graph,
                R.id.navigation_dashboard,
                R.id.navigation_metrics,
                R.id.navigation_preferences
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        findViewById<BottomNavigationView>(R.id.nav_view).let {
            it.setupWithNavController(navController)
            it.selectedItemId = R.id.navigation_gauge
        }

        val mainActivityPreferences = getMainActivityPreferences()
        findViewById<BottomNavigationView>(R.id.nav_view).menu.run {
            findItem(R.id.navigation_dashboard).isVisible =
                mainActivityPreferences.showDashView

            findItem(R.id.navigation_gauge).isVisible =
                mainActivityPreferences.showGaugeView

            findItem(R.id.navigation_metrics).isVisible =
                mainActivityPreferences.showMetricsView

            findItem(R.id.navigation_graph).isVisible =
                mainActivityPreferences.showGraphView
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomAppBar {
                it.menu.findItem(R.id.ctx_menu_dtc).isVisible = DataLogger.instance.isDTCEnabled()
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
            Log.i(ACTIVITY_LOGGER_TAG, "Start data logging")
            DataLoggerService.start()
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
                    screenId?.let { screenId ->
                        navigateToPreferencesScreen(screenId)
                    }
                }

                R.id.ctx_menu_pids_to_display -> {
                    val screenId = when (getCurrentScreenId()) {
                        R.id.navigation_gauge -> "pref.gauge.displayed_parameter_ids"
                        R.id.navigation_graph -> "pref.graph.displayed_parameter_ids"
                        else -> null
                    }
                    screenId?.let { screenId ->
                        navigateToPreferencesScreen(screenId)
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

                R.id.ctx_menu_view_configuration -> {
                    navigateToPreferencesScreen(
                        when (getCurrentScreenId()) {
                            R.id.navigation_dashboard -> "pref.dashboard"
                            R.id.navigation_gauge -> "pref.gauge"
                            R.id.navigation_graph -> "pref.graph"
                            R.id.navigation_metrics -> "pref.metrics"
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
    val bottomNavigationView: BottomNavigationView = findViewById(R.id.nav_view)
    val selectedItemId: Int = bottomNavigationView.selectedItemId
    val currentView: MenuItem =
        bottomNavigationView.menu.findItem(selectedItemId)
    return currentView.itemId
}