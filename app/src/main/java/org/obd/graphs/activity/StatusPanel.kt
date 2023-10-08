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

import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.profile.getSelectedProfileName
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.COLOR_RAINBOW_INDIGO
import org.obd.graphs.ui.common.TOGGLE_TOOLBAR_ACTION
import org.obd.graphs.ui.common.highLightText


internal fun MainActivity.updateAdapterConnectionType() {
    updateTextField(
        R.id.connection_status,
        resources.getString(R.string.adapter_connection_type),
        dataLoggerPreferences.instance.connectionType,
        COLOR_PHILIPPINE_GREEN,
        1.0f
    )
}

internal fun MainActivity.setupStatusPanel() {
    updateAdapterConnectionType()
    updateVehicleProfile()

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
        resources.getString(R.string.vehicle_profile),
        getSelectedProfileName()!!,
        COLOR_RAINBOW_INDIGO,
        1.0f
    )
}

private fun MainActivity.updateTextField(
    viewId: Int,
    text1: String,
    text2: String,
    color: Int,
    text2Size: Float
) {
    (findViewById<TextView>(viewId)).let {
        it.text = "$text1 $text2"
        it.highLightText(text1, 0.7f, Color.WHITE)
        it.highLightText(text2, text2Size, color)
    }
}