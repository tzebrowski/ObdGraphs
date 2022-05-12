package org.openobd2.core.logger

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.openobd2.core.logger.bl.datalogger.*
import org.openobd2.core.logger.bl.trip.TripRecorderBroadcastReceiver
import org.openobd2.core.logger.ui.common.TOGGLE_TOOLBAR_ACTION
import org.openobd2.core.logger.ui.common.toast
import org.openobd2.core.logger.ui.preferences.*

internal val tripRecorderBroadcastReceiver = TripRecorderBroadcastReceiver()
internal val powerReceiver = PowerBroadcastReceiver()

internal fun MainActivity.receive(context: Context?, intent: Intent?) {
    when (intent?.action) {
        TOGGLE_TOOLBAR_ACTION -> {
            if (getMainActivityPreferences().hideToolbarDoubleClick) {
                val layout: CoordinatorLayout = findViewById(R.id.coordinator_Layout)
                layout.isVisible = !layout.isVisible
            }
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
        NOTIFICATION_METRICS_VIEW_TOGGLE -> {
            toggleNavigationItem(R.id.navigation_metrics)
        }

        NOTIFICATION_GRAPH_VIEW_TOGGLE -> {
            toggleNavigationItem(R.id.navigation_graph)
        }

        NOTIFICATION_DEBUG_VIEW_TOGGLE -> {
            toggleNavigationItem(R.id.navigation_debug)
        }

        NOTIFICATION_DASH_VIEW_TOGGLE -> {
            toggleNavigationItem(R.id.navigation_dashboard)
        }

        NOTIFICATION_GAUGE_VIEW_TOGGLE -> {
            toggleNavigationItem(R.id.navigation_gauge)
        }

        DATA_LOGGER_CONNECTING_EVENT -> {
            toast(R.string.main_activity_toast_connection_connecting)
            val progressBar: ProgressBar = findViewById(R.id.p_bar)
            progressBar.visibility = View.VISIBLE
            progressBar.indeterminateDrawable.colorFilter = PorterDuffColorFilter(
                Color.parseColor("#C22636"),
                PorterDuff.Mode.SRC_IN
            )

            val btn: FloatingActionButton = findViewById(R.id.connect_btn)
            btn.backgroundTintList =
                ContextCompat.getColorStateList(applicationContext, R.color.purple_200)
            btn.setOnClickListener {
                Log.i(ACTIVITY_LOGGER_TAG, "Stop data logging ")
                DataLoggerService.stopAction(context!!)
            }
            btn.refreshDrawableState()
        }

        DATA_LOGGER_CONNECTED_EVENT -> {
            toast(R.string.main_activity_toast_connection_established)

            val progressBar: ProgressBar = findViewById(R.id.p_bar)
            progressBar.indeterminateDrawable.colorFilter = PorterDuffColorFilter(
                Color.parseColor("#01804F"),
                PorterDuff.Mode.SRC_IN
            )

            if (getMainActivityPreferences().hideToolbarConnected) {
                val layout: CoordinatorLayout = findViewById(R.id.coordinator_Layout)
                layout.isVisible = false
            }
        }

        DATA_LOGGER_STOPPED_EVENT -> {
            toast(R.string.main_activity_toast_connection_stopped)
            handleStop(context!!)
        }

        DATA_LOGGER_ERROR_EVENT -> {
            toast(R.string.main_activity_toast_connection_error)
            handleStop(context!!)
        }
    }
}

private fun MainActivity.handleStop(context: Context) {
    val progressBar: ProgressBar = findViewById(R.id.p_bar)
    progressBar.visibility = View.GONE

    val btn: FloatingActionButton = findViewById(R.id.connect_btn)
    btn.backgroundTintList =
        ContextCompat.getColorStateList(applicationContext, R.color.purple_500)
    btn.setOnClickListener {
        Log.i(ACTIVITY_LOGGER_TAG, "Stop data logging ")
        DataLoggerService.startAction(context)
    }

    if (getMainActivityPreferences().hideToolbarConnected) {
        val layout: CoordinatorLayout = findViewById(R.id.coordinator_Layout)
        layout.isVisible = true
    }
}

private fun MainActivity.toggleNavigationItem(id: Int) {
    findViewById<BottomNavigationView>(R.id.nav_view).menu.findItem(id)?.run {
        this.isVisible = !this.isVisible
    }
}

internal fun MainActivity.registerReceiver() {

    registerReceiver(activityBroadcastReceiver, IntentFilter().apply {
        addAction(DATA_LOGGER_CONNECTING_EVENT)
        addAction(DATA_LOGGER_STOPPED_EVENT)
        addAction(DATA_LOGGER_STOPPING_EVENT)
        addAction(DATA_LOGGER_ERROR_EVENT)
        addAction(DATA_LOGGER_CONNECTED_EVENT)
        addAction(Intent.ACTION_BATTERY_CHANGED)
        addAction(DATA_LOGGER_ERROR_CONNECT_EVENT)
        addAction(NOTIFICATION_GRAPH_VIEW_TOGGLE)
        addAction(NOTIFICATION_DEBUG_VIEW_TOGGLE)
        addAction(NOTIFICATION_GAUGE_VIEW_TOGGLE)
        addAction(NOTIFICATION_DASH_VIEW_TOGGLE)
        addAction(NOTIFICATION_METRICS_VIEW_TOGGLE)
        addAction(TOGGLE_TOOLBAR_ACTION)
        addAction(SCREEN_OFF_EVENT)
        addAction(SCREEN_ON_EVENT)
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