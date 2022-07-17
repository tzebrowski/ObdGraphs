package org.obd.graphs.activity

import android.util.Log
import android.view.MenuItem
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.obd.graphs.ApplicationContext
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLoggerService
import org.obd.graphs.ui.preferences.PREFERENCE_SCREEN_KEY


fun navigateToPreferencesScreen(prefKey: String) {
    (ApplicationContext.get() as MainActivity).navController()
        .navigate(
            R.id.navigation_preferences,
            bundleOf(PREFERENCE_SCREEN_KEY to prefKey)
        )
}

internal fun MainActivity.setupNavigationBar() {
    val navView: BottomNavigationView = findViewById(R.id.nav_view)
    val navController = navController()

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
    navView.setupWithNavController(navController)

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

    navView.selectedItemId = R.id.navigation_gauge

    navController.addOnDestinationChangedListener { _, destination, _ ->
        currentNavTabName = destination.label.toString()
    }
}

internal fun MainActivity.setupNavigationBarButtons() {

    val btnStart: FloatingActionButton = findViewById(R.id.connect_btn)
    btnStart.setOnClickListener {
        Log.i(ACTIVITY_LOGGER_TAG, "Start data logging")
        DataLoggerService.start()
    }

    val menuButton: FloatingActionButton = findViewById(R.id.menu_btn)
    menuButton.setOnClickListener {
        val popupMenu = PopupMenu(this, menuButton)
        popupMenu.menuInflater.inflate(R.menu.context_menu, popupMenu.menu)

        when (currentNavTabName) {
            resources.getString(R.string.navigation_title_graph) -> {
                popupMenu.menu.findItem(R.id.ctx_menu_view_custom_action_1).isVisible = true
                popupMenu.menu.findItem(R.id.ctx_menu_view_custom_action_1).title = resources.getString(R.string.pref_graph_trips_selected)
            }
            else -> {
                popupMenu.menu.findItem(R.id.ctx_menu_view_custom_action_1).isVisible = false
            }
        }

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {

                R.id.ctx_menu_view_custom_action_1 -> {
                    val screenId = when (getCurrentScreenId()) {
                        R.id.navigation_graph -> "pref.gauge.recordings"
                        else -> null
                    }
                    screenId?.let {
                        navigateToPreferencesScreen(it)
                    }
                }

                R.id.ctx_menu_pids_to_display -> {
                    val screenId = when (getCurrentScreenId()) {
                        R.id.navigation_gauge -> "pref.gauge.displayed_parameter_ids"
                        R.id.navigation_graph -> "pref.graph.displayed_parameter_ids"
                        else -> null
                    }
                    screenId?.let {
                        navigateToPreferencesScreen(it)
                    }
                }

                R.id.ctx_menu_pids_to_query -> {
                    navigateToPreferencesScreen("pref.registry")
                }

                R.id.ctx_menu_view_profiles -> {
                    navigateToPreferencesScreen("pref.profiles")
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
        popupMenu.show()
    }
}

private fun MainActivity.navController(): NavController =
    (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

private fun MainActivity.getCurrentScreenId(): Int {
    val bottomNavigationView: BottomNavigationView = findViewById(R.id.nav_view)
    val selectedItemId: Int = bottomNavigationView.selectedItemId
    val currentView: MenuItem =
        bottomNavigationView.menu.findItem(selectedItemId)
    return currentView.itemId
}