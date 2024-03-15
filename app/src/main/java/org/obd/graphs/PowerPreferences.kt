/**
 * Copyright 2019-2024, Tomasz Żebrowski
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
package org.obd.graphs

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.isEnabled

data class PowerPreferences(
    var connectOnPower: Boolean = false,
    var screenOnOff: Boolean = false,
    var switchNetworkOffOn: Boolean = false,
    var startDataLoggingAfter: Long = 10
)

private val powerPreferences = PowerPreferences()

fun getPowerPreferences(): PowerPreferences  = powerPreferences.apply {
     switchNetworkOffOn = Prefs.isEnabled("pref.adapter.power.switch_network_on_off")
     screenOnOff = Prefs.isEnabled("pref.adapter.power.screen_off")
     connectOnPower = Prefs.isEnabled("pref.adapter.power.connect_adapter")
     startDataLoggingAfter = Prefs.getString("pref.adapter.power.start_data_logging.after", "10")!!.toLong()
}
