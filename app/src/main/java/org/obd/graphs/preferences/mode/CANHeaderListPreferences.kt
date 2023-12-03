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
package org.obd.graphs.preferences.mode

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.obd.graphs.CAN_HEADER_COUNTER_PREF
import org.obd.graphs.MODE_HEADER_PREFIX
import org.obd.graphs.PREFERENCE_PAGE
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.getCurrentMode
import org.obd.graphs.preferences.Prefs

class CANHeaderListPreferences(
    context: Context,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {

    private val preferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
        Log.i(MODE_LOG_KEY, "Updating mode name: ${getCurrentMode()}=$newValue")
        Prefs.edit()
            .putString("$MODE_HEADER_PREFIX.${getCurrentMode()}", newValue.toString())
            .apply()
        navigateToPreferencesScreen(PREFERENCE_PAGE)
        true
    }

    init {
        onPreferenceChangeListener = preferenceChangeListener

        linkedMapOf(
            "" to "",
            "DA10F1" to "DA10F1",
            "DB33F1" to "DB33F1",
            "DA60F1" to "DA60F1",
            "DA18F1" to "DA18F1",
            "DA17F1" to "DA17F1",
            "DAF110" to "DAF110",
            "DAC7F1" to "DAC7F1",
            "7DF" to "7DF",
            "18DA18F1" to "18DA18F1",
            "18DA18F1" to "18DA18F1",
            "18DA60F1" to "18DA60F1",
            "18DA10F1" to "18DA10F1",
            "18DB33F1" to "18DB33F1",
            "18DA17F1" to "18DA17F1",
            "18DAF110" to "18DAF110",
            "18DAC7F1" to "18DAC7F1",


            ).apply {
            Prefs.getInt(CAN_HEADER_COUNTER_PREF, 0).let { it ->
                Log.d(MODE_LOG_KEY, "Number of custom CAN headers available: $it")
                if (it > 0) {
                    (1..it).forEach {
                        Prefs.getString("pref.adapter.init.header.$it", "")?.let { header ->
                            this[header] = header
                        }
                    }
                }
            }
        }.let {
            setDefaultValue(it.keys.first())
            entries = it.values.toTypedArray()
            entryValues = it.keys.toTypedArray()
        }
    }
}