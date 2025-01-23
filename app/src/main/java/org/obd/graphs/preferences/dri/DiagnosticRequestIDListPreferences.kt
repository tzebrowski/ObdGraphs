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
package org.obd.graphs.preferences.dri

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.obd.graphs.*
import org.obd.graphs.activity.navigateToPreferencesScreen

class DiagnosticRequestIDListPreferences(
    context: Context,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {

    private val preferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
        diagnosticRequestIDMapper.setCurrentMapping(newValue.toString())
        navigateToPreferencesScreen(PREFERENCE_PAGE)
        true
    }

    init {

        onPreferenceChangeListener = preferenceChangeListener

        diagnosticRequestIDMapper.getAvailableKeys().associateWith {  diagnosticRequestIDMapper.getKeyById(it) }.let {
            setDefaultValue(it.keys.first())
            entries = it.values.toTypedArray()
            entryValues = it.keys.toTypedArray()
        }
    }
}