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
package org.obd.graphs.preferences

import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import org.obd.graphs.activity.*
import org.obd.graphs.sendBroadcastEvent

const val PREFERENCE_CONNECTION_TYPE = "pref.adapter.connection.type"

internal fun PreferencesFragment.registerViewsPreferenceChangeListeners() {
    registerCheckboxListener(
        GRAPH_VIEW_ID,
        NOTIFICATION_GRAPH_VIEW_TOGGLE
    )
    registerCheckboxListener(
        GAUGE_VIEW_ID,
        NOTIFICATION_GAUGE_VIEW_TOGGLE
    )
    registerCheckboxListener(
        DASH_VIEW_ID,
        NOTIFICATION_DASH_VIEW_TOGGLE
    )

    registerCheckboxListener(
        GIULIA_VIEW_ID,
        NOTIFICATION_GIULIA_VIEW_TOGGLE
    )
}

fun updateToolbar() {
    sendBroadcastEvent(NOTIFICATION_GRAPH_VIEW_TOGGLE)
    sendBroadcastEvent(NOTIFICATION_DASH_VIEW_TOGGLE)
    sendBroadcastEvent(NOTIFICATION_GAUGE_VIEW_TOGGLE)
    sendBroadcastEvent(NOTIFICATION_GIULIA_VIEW_TOGGLE)
}

private fun PreferencesFragment.registerCheckboxListener(key: String, actionName: String) {
    val preference = findPreference<CheckBoxPreference>(key)
    preference?.onPreferenceChangeListener =
        Preference.OnPreferenceChangeListener { _, _ ->
            sendBroadcastEvent(actionName)
            true
        }
}