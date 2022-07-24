package org.obd.graphs.activity

import android.content.Intent
import android.content.IntentFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentContainerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.obd.graphs.*
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.bl.trip.TripRecorderBroadcastReceiver
import org.obd.graphs.ui.common.*
import org.obd.graphs.ui.preferences.Prefs

internal val tripRecorderBroadcastReceiver = TripRecorderBroadcastReceiver()
internal val powerReceiver = PowerBroadcastReceiver()

const val NOTIFICATION_GRAPH_VIEW_TOGGLE = "view.graph.toggle"
const val NOTIFICATION_DASH_VIEW_TOGGLE = "view.dash.toggle"
const val NOTIFICATION_GAUGE_VIEW_TOGGLE = "view.gauge.toggle"
const val NOTIFICATION_METRICS_VIEW_TOGGLE = "view.metrics.toggle"
const val GRAPH_VIEW_ID = "pref.graph.view.enabled"
const val GAUGE_VIEW_ID = "pref.gauge.view.enabled"
const val DASH_VIEW_ID = "pref.dash.view.enabled"
const val METRICS_VIEW_ID = "pref.metrics.view.enabled"
const val DATA_LOGGER_PROCESS_IS_RUNNING = "cache.graph.collecting_process_is_running"

internal fun MainActivity.receive(intent: Intent?) {
    when (intent?.action) {
        TOGGLE_TOOLBAR_ACTION -> {
            if (getMainActivityPreferences().hideToolbarDoubleClick) {
                val coordinatorLayout: CoordinatorLayout = findViewById(R.id.coordinator_Layout)
                coordinatorLayout.isVisible = !coordinatorLayout.isVisible
                findViewById<FragmentContainerView>(R.id.nav_host_fragment).let {
                    val param = it.layoutParams as LinearLayout.LayoutParams
                    if (coordinatorLayout.isVisible) {
                        param.weight = 7.2f
                    } else {
                        param.weight = 9.0f
                    }
                }
            }
        }
        PROFILE_CHANGED_EVENT -> {
            updateVehicleProfile()
        }
        SCREEN_OFF_EVENT -> {
            lockScreen()
        }
        SCREEN_ON_EVENT -> {
            Log.i(ACTIVITY_LOGGER_TAG, "Activating application.")
            changeScreenBrightness(1f)
        }
        DATA_LOGGER_ERROR_CONNECT_EVENT -> {
            toast(R.string.main_activity_toast_connection_connect_error)
        }

        DATA_LOGGER_ADAPTER_NOT_SET_EVENT -> {
            navigateToPreferencesScreen("pref.adapter")
            toast(R.string.main_activity_toast_adapter_is_not_selected)
        }

        NOTIFICATION_METRICS_VIEW_TOGGLE -> {
            toggleNavigationItem(METRICS_VIEW_ID, R.id.navigation_metrics)
        }

        NOTIFICATION_GRAPH_VIEW_TOGGLE -> {
            toggleNavigationItem(GRAPH_VIEW_ID, R.id.navigation_graph)
        }

        NOTIFICATION_DASH_VIEW_TOGGLE -> {
            toggleNavigationItem(DASH_VIEW_ID, R.id.navigation_dashboard)
        }

        NOTIFICATION_GAUGE_VIEW_TOGGLE -> {
            toggleNavigationItem(GAUGE_VIEW_ID, R.id.navigation_gauge)
        }

        DATA_LOGGER_CONNECTING_EVENT -> {
            toast(R.string.main_activity_toast_connection_connecting)
            val progressBar: ProgressBar = findViewById(R.id.p_bar)
            progressBar.visibility = View.VISIBLE
            progressBar.indeterminateDrawable.colorFilter = PorterDuffColorFilter(
                COLOR_CARDINAL,
                PorterDuff.Mode.SRC_IN
            )

            val btn: FloatingActionButton = findViewById(R.id.connect_btn)
            btn.backgroundTintList =
                ContextCompat.getColorStateList(applicationContext, R.color.cardinal)
            btn.setOnClickListener {
                Log.i(ACTIVITY_LOGGER_TAG, "Stop data logging ")
                DataLoggerService.stop()
            }
            btn.refreshDrawableState()
            Cache[DATA_LOGGER_PROCESS_IS_RUNNING] = true
        }

        DATA_LOGGER_NO_NETWORK_EVENT -> {
            toast(R.string.main_activity_toast_connection_no_network)
            handleStop()
        }

        DATA_LOGGER_CONNECTED_EVENT -> {
            toast(R.string.main_activity_toast_connection_established)

            val progressBar: ProgressBar = findViewById(R.id.p_bar)
            progressBar.indeterminateDrawable.colorFilter = PorterDuffColorFilter(
                COLOR_PHILIPPINE_GREEN,
                PorterDuff.Mode.SRC_IN
            )

            if (getMainActivityPreferences().hideToolbarConnected) {
               findViewById<CoordinatorLayout>(R.id.coordinator_Layout).isVisible = false
            }

            connectionStatusConnected()

            findViewById<FragmentContainerView>(R.id.nav_host_fragment).let {
                val param = it.layoutParams as LinearLayout.LayoutParams
                param.weight = 9.0f
            }
        }

        DATA_LOGGER_STOPPED_EVENT -> {
            toast(R.string.main_activity_toast_connection_stopped)
            handleStop()
            Cache[DATA_LOGGER_PROCESS_IS_RUNNING] = false


        }

        DATA_LOGGER_ERROR_EVENT -> {
            toast(R.string.main_activity_toast_connection_error)
            handleStop()
        }
    }
}



private fun MainActivity.handleStop() {
    val progressBar: ProgressBar = findViewById(R.id.p_bar)
    progressBar.visibility = View.GONE

    val btn: FloatingActionButton = findViewById(R.id.connect_btn)
    btn.backgroundTintList =
        ContextCompat.getColorStateList(applicationContext, R.color.philippine_green)
    btn.setOnClickListener {
        Log.i(ACTIVITY_LOGGER_TAG, "Stop data logging ")
        DataLoggerService.start()
    }

    if (getMainActivityPreferences().hideToolbarConnected) {
        val layout: CoordinatorLayout = findViewById(R.id.coordinator_Layout)
        layout.isVisible = true
    }

    findViewById<FragmentContainerView>(R.id.nav_host_fragment).let {
        val param = it.layoutParams as LinearLayout.LayoutParams
        param.weight = 7.0f
    }

    connectionStatusDisconnected()
}

internal fun MainActivity.toggleNavigationItem(prefKey: String, id: Int) {
    findViewById<BottomNavigationView>(R.id.nav_view).menu.findItem(id)?.run {
        this.isVisible = Prefs.getBoolean(prefKey, true)
    }
}

internal fun MainActivity.unregisterReceiver() {
    unregisterReceiver(activityBroadcastReceiver)
    unregisterReceiver(tripRecorderBroadcastReceiver)
    unregisterReceiver(powerReceiver)
}

internal fun MainActivity.registerReceiver() {

    registerReceiver(activityBroadcastReceiver, IntentFilter().apply {
        addAction(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
        addAction(DATA_LOGGER_CONNECTING_EVENT)
        addAction(DATA_LOGGER_STOPPED_EVENT)
        addAction(DATA_LOGGER_STOPPING_EVENT)
        addAction(DATA_LOGGER_ERROR_EVENT)
        addAction(DATA_LOGGER_CONNECTED_EVENT)
        addAction(DATA_LOGGER_NO_NETWORK_EVENT)
        addAction(Intent.ACTION_BATTERY_CHANGED)
        addAction(DATA_LOGGER_ERROR_CONNECT_EVENT)
        addAction(NOTIFICATION_GRAPH_VIEW_TOGGLE)
        addAction(NOTIFICATION_GAUGE_VIEW_TOGGLE)
        addAction(NOTIFICATION_DASH_VIEW_TOGGLE)
        addAction(NOTIFICATION_METRICS_VIEW_TOGGLE)
        addAction(TOGGLE_TOOLBAR_ACTION)
        addAction(SCREEN_OFF_EVENT)
        addAction(SCREEN_ON_EVENT)
        addAction(PROFILE_CHANGED_EVENT)
    })

    registerReceiver(tripRecorderBroadcastReceiver, IntentFilter().apply {
        addAction(DATA_LOGGER_CONNECTED_EVENT)
        addAction(DATA_LOGGER_STOPPED_EVENT)
    })

    registerReceiver(powerReceiver, IntentFilter().apply {
        addAction("android.intent.action.ACTION_POWER_CONNECTED")
        addAction("android.intent.action.ACTION_POWER_DISCONNECTED")
    })
}