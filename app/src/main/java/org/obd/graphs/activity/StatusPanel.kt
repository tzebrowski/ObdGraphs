 /**
 * Copyright 2019-2025, Tomasz Żebrowski
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

import android.annotation.SuppressLint
import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.profile.profile
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.*


internal fun MainActivity.updateVehicleStatus(status: String) {

    updateTextField(
        R.id.vehicle_status,
        resources.getString(R.string.status_panel_vehicle_status),
        status,
        COLOR_CARDINAL,
        1.0f
    ){
        it.isGone = !dataLoggerPreferences.instance.vehicleStatusPanelEnabled
    }
}

internal fun MainActivity.updateAdapterConnectionType() {
    updateTextField(
        R.id.connection_status,
        resources.getString(R.string.status_panel_adapter_connection_type),
        dataLoggerPreferences.instance.connectionType,
        COLOR_PHILIPPINE_GREEN,
        1.0f
    )
}

internal fun MainActivity.setupStatusPanel() {
    updateAdapterConnectionType()
    updateVehicleProfile()
    updateVehicleStatus("")

    (findViewById<TextView>(R.id.connection_status)).let {
        it.setOnClickListener {
            navigateToPreferencesScreen("pref.adapter.connection")
        }
    }

    (findViewById<TextView>(R.id.vehicle_profile)).let {
        it.setOnClickListener {
            navigateToPreferencesScreen("pref.profiles")
        }
    }

    (findViewById<ImageView>(R.id.toggle_fullscreen)).let {
        it.setOnClickListener {
           sendBroadcastEvent(TOGGLE_TOOLBAR_ACTION)
        }
    }
}

internal fun MainActivity.updateVehicleProfile() {
    updateTextField(
        R.id.vehicle_profile,
        resources.getString(R.string.status_panel_vehicle_profile),
        profile.getCurrentProfileName(),
        COLOR_RAINBOW_INDIGO,
        1.0f
    )
}

@SuppressLint("SetTextI18n")
private fun MainActivity.updateTextField(
    viewId: Int,
    text1: String,
    text2: String,
    color: Int,
    text2Size: Float,
    func: (p: TextView) -> Unit = {}
) {
    (findViewById<TextView>(viewId)).let {
        func(it)
        it.text = "$text1 $text2"
        it.highLightText(text1, 0.7f, Color.WHITE)
        it.highLightText(text2, text2Size, color)
    }
}
