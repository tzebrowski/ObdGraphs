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
import org.obd.graphs.*
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.preferences.Prefs

class ModeListPreferences(
    context: Context,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {

    private val preferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
        val modeId = Prefs.getString("$MODE_NAME_PREFIX.$newValue", "")
        val modeHeader = Prefs.getString("$MODE_HEADER_PREFIX.$newValue", "")

        Log.i(MODE_LOG_KEY, "Updating mode $modeId=$modeHeader")

        Prefs.edit().run {
            putString(PREF_ADAPTER_MODE_ID_EDITOR, modeId)
            putString(PREF_CAN_HEADER_EDITOR, modeHeader)
            apply()
        }

        navigateToPreferencesScreen(PREFERENCE_PAGE)
        true
    }

    init {

        onPreferenceChangeListener = preferenceChangeListener

        getAvailableModes().associateWith { Prefs.getString("$MODE_NAME_PREFIX.$it", "") }.let {
            setDefaultValue(it.keys.first())
            entries = it.values.toTypedArray()
            entryValues = it.keys.toTypedArray()
        }
    }
}