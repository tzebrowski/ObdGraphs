 /**
 * Copyright 2019-2026, Tomasz Żebrowski
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
package org.obd.graphs.preferences.pid

import android.content.Context
import android.util.AttributeSet
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.obd.graphs.AA_HIGH_FREQ_PID_SELECTION_CHANGED_EVENT
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.sendBroadcastEvent

class IndividualQueryStrategyCheckBoxPreference(
    context: Context,
    attrs: AttributeSet?,
) : CheckBoxPreference(context, attrs) {
    init {
        onPreferenceChangeListener =
            OnPreferenceChangeListener { _, _ ->
                navigateToPreferencesScreen("pref.registry")
                sendBroadcastEvent(AA_HIGH_FREQ_PID_SELECTION_CHANGED_EVENT)
                true
            }
    }
}
