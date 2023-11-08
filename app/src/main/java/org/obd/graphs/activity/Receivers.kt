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

import android.content.Intent
import android.content.IntentFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.obd.graphs.*
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.bl.trip.TripManagerBroadcastReceiver
import org.obd.graphs.preferences.PREFS_CONNECTION_TYPE_CHANGED_EVENT
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.isEnabled
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.TOGGLE_TOOLBAR_ACTION
import org.obd.graphs.ui.common.toast


internal val tripRecorderBroadcastReceiver = TripManagerBroadcastReceiver()
internal val powerReceiver = PowerBroadcastReceiver()

const val NOTIFICATION_GRAPH_VIEW_TOGGLE = "view.graph.toggle"
const val NOTIFICATION_DASH_VIEW_TOGGLE = "view.dash.toggle"
const val NOTIFICATION_GAUGE_VIEW_TOGGLE = "view.gauge.toggle"
const val NOTIFICATION_GIULIA_VIEW_TOGGLE = "view.metrics.toggle"
const val GRAPH_VIEW_ID = "pref.graph.view.enabled"
const val GAUGE_VIEW_ID = "pref.gauge.view.enabled"
const val DASH_VIEW_ID = "pref.dash.view.enabled"
const val GIULIA_VIEW_ID = "pref.giulia.view.enabled"

internal fun MainActivity.receive(intent: Intent?) {

    when (intent?.action) {
        REQUEST_LOCATION_PERMISSIONS ->{
            permissions.requestLocationPermissions(this)
        }

        DATA_LOGGER_WIFI_NOT_CONNECTED -> {
            getContext()?.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            toast(R.string.main_activity_toast_connection_wifi_not_connected)
        }

        DATA_LOGGER_WIFI_INCORRECT -> {
            getContext()?.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            toast(R.string.main_activity_toast_connection_wifi_incorrect_ssid)
        }
        SCREEN_LOCK_PROGRESS_EVENT -> {
            lockScreenDialogShow { dialogTitle ->
                var msg = intent.getExtraParam()
                if (msg.isEmpty()){
                    msg = getText(R.string.dialog_screen_lock_message) as String
                }
                dialogTitle.text = msg
            }
        }
        AA_EDIT_PREF_SCREEN -> {
            navigateToPreferencesScreen("pref.aa")
        }

        UsbManager.ACTION_USB_DEVICE_DETACHED -> {
            val usbDevice: UsbDevice = intent.extras?.get(UsbManager.EXTRA_DEVICE) as UsbDevice
            toast(R.string.pref_usb_device_detached,usbDevice.productName!!)
            dataLogger.stop()
        }

        USB_DEVICE_ATTACHED_EVENT -> {
            val usbDevice: UsbDevice = intent.extras?.get(UsbManager.EXTRA_DEVICE) as UsbDevice
            toast(R.string.pref_usb_device_attached,usbDevice.productName!!)
        }

        SCREEN_UNLOCK_PROGRESS_EVENT -> {
            lockScreenDialog.dismiss()
        }

        DATA_LOGGER_DTC_AVAILABLE -> {
            if (Prefs.isEnabled("pref.dtc.show_notification")) {
                toast(R.string.pref_pids_group_dtc_available_message)
            }
        }

        REQUEST_PERMISSIONS_BT -> {
            permissions.requestBluetoothPermissions(this)
        }


        TOGGLE_TOOLBAR_ACTION -> {
            toolbar {
                if (getMainActivityPreferences().hideToolbarDoubleClick) {
                    it.isVisible = !it.isVisible
                }
            }
        }

        PROFILE_CHANGED_EVENT -> {
            updateVehicleProfile()
            updateAdapterConnectionType()
        }
        SCREEN_OFF_EVENT -> {
            lockScreen()
        }
        SCREEN_ON_EVENT -> {
            Log.i(LOG_TAG, "Activating application.")
            changeScreenBrightness(1f)
        }
        DATA_LOGGER_ERROR_CONNECT_EVENT -> {
            toast(R.string.main_activity_toast_connection_connect_error)
        }

        DATA_LOGGER_ADAPTER_NOT_SET_EVENT -> {
            navigateToPreferencesScreen("pref.adapter.connection")
            toast(R.string.main_activity_toast_adapter_is_not_selected)
        }

        NOTIFICATION_GIULIA_VIEW_TOGGLE -> {
            toggleNavigationItem(GIULIA_VIEW_ID, R.id.navigation_giulia)
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

            progressBar {
                it.visibility = View.VISIBLE
                it.indeterminateDrawable.colorFilter = PorterDuffColorFilter(
                    COLOR_CARDINAL,
                    PorterDuff.Mode.SRC_IN
                )
            }

            floatingActionButton {
                it.backgroundTintList =
                    ContextCompat.getColorStateList(applicationContext, R.color.cardinal)
                it.setOnClickListener {
                    Log.i(LOG_TAG, "Stop data logging ")
                    dataLogger.stop()
                }
                it.refreshDrawableState()
            }
        }

        PREFS_CONNECTION_TYPE_CHANGED_EVENT -> {
            updateAdapterConnectionType()
        }

        DATA_LOGGER_NO_NETWORK_EVENT -> {
            toast(R.string.main_activity_toast_connection_no_network)
            handleStop()
        }

        DATA_LOGGER_CONNECTED_EVENT -> {
            toast(R.string.main_activity_toast_connection_established)

            progressBar {
                it.indeterminateDrawable.colorFilter = PorterDuffColorFilter(
                    COLOR_PHILIPPINE_GREEN,
                    PorterDuff.Mode.SRC_IN
                )
            }

            timer {
                it.isCountDown = false
                it.base = SystemClock.elapsedRealtime()
                it.start()
            }

            toolbar {
                if (getMainActivityPreferences().hideToolbarConnected) {
                    it.isVisible = false
                }
            }

            updateAdapterConnectionType()
        }

        DATA_LOGGER_STOPPED_EVENT -> {
            toast(R.string.main_activity_toast_connection_stopped)
            handleStop()
        }

        DATA_LOGGER_ERROR_EVENT -> {
            toast(R.string.main_activity_toast_connection_error)
            handleStop()
        }
    }
}

private fun MainActivity.handleStop() {

    progressBar {
        it.visibility = View.GONE
    }

    floatingActionButton {
        it.backgroundTintList =
            ContextCompat.getColorStateList(applicationContext, R.color.philippine_green)
        it.setOnClickListener {
            Log.i(LOG_TAG, "Stop data logging ")
            dataLogger.start()
        }
    }

    toolbar {
        if (getMainActivityPreferences().hideToolbarConnected) {
            it.isVisible = true
        }
    }

    timer {
        it.stop()
    }
}

internal fun MainActivity.toggleNavigationItem(prefKey: String, id: Int) {
    findViewById<BottomNavigationView>(R.id.bottom_nav_view).menu.findItem(id)?.run {
        this.isVisible = Prefs.getBoolean(prefKey, true)
    }
}

internal fun MainActivity.unregisterReceiver() {
    unregisterReceiver(activityBroadcastReceiver)
    unregisterReceiver(tripRecorderBroadcastReceiver)
    unregisterReceiver(powerReceiver)
    unregisterReceiver(dataLogger.eventsReceiver)
}

internal fun MainActivity.registerReceiver() {

    registerReceiver(activityBroadcastReceiver, IntentFilter().apply {
        addAction(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)
        addAction(DATA_LOGGER_CONNECTING_EVENT)
        addAction(DATA_LOGGER_STOPPED_EVENT)
        addAction(DATA_LOGGER_ERROR_EVENT)
        addAction(DATA_LOGGER_CONNECTED_EVENT)
        addAction(DATA_LOGGER_NO_NETWORK_EVENT)
        addAction(DATA_LOGGER_DTC_AVAILABLE)
        addAction(Intent.ACTION_BATTERY_CHANGED)
        addAction(DATA_LOGGER_ERROR_CONNECT_EVENT)
        addAction(NOTIFICATION_GRAPH_VIEW_TOGGLE)
        addAction(NOTIFICATION_GAUGE_VIEW_TOGGLE)
        addAction(NOTIFICATION_DASH_VIEW_TOGGLE)
        addAction(NOTIFICATION_GIULIA_VIEW_TOGGLE)
        addAction(TOGGLE_TOOLBAR_ACTION)
        addAction(SCREEN_OFF_EVENT)
        addAction(SCREEN_ON_EVENT)
        addAction(PROFILE_CHANGED_EVENT)
        addAction(REQUEST_PERMISSIONS_BT)
        addAction(PREFS_CONNECTION_TYPE_CHANGED_EVENT)
        addAction(SCREEN_LOCK_PROGRESS_EVENT)
        addAction(SCREEN_UNLOCK_PROGRESS_EVENT)
        addAction(USB_DEVICE_ATTACHED_EVENT)
        addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        addAction(AA_EDIT_PREF_SCREEN)
        addAction(DATA_LOGGER_WIFI_INCORRECT)
        addAction(DATA_LOGGER_WIFI_NOT_CONNECTED)
        addAction(REQUEST_LOCATION_PERMISSIONS)
    })

    registerReceiver(tripRecorderBroadcastReceiver, IntentFilter().apply {
        addAction(DATA_LOGGER_CONNECTED_EVENT)
        addAction(DATA_LOGGER_STOPPED_EVENT)
        addAction(DATA_LOGGER_CONNECTING_EVENT)
        addAction(TRIP_LOAD_EVENT)
    })

    registerReceiver(powerReceiver, IntentFilter().apply {
        addAction("android.intent.action.ACTION_POWER_CONNECTED")
        addAction("android.intent.action.ACTION_POWER_DISCONNECTED")
    })

    registerReceiver(dataLogger.eventsReceiver, IntentFilter().apply {
        addAction(RESOURCE_LIST_CHANGED_EVENT)
        addAction(PROFILE_CHANGED_EVENT)
    })
}