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
import android.util.Log
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.SwitchPreferenceCompat
import org.obd.graphs.ACCESS_EXTERNAL_STORAGE_ENABLED
import org.obd.graphs.MODULES_LIST_CHANGED_EVENT
import org.obd.graphs.PREF_MODULE_LIST
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.modules
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.sendBroadcastEvent

private const val LOG_TAG = "ModulesListPreferences"

class ModulesListPreferences(
    context: Context,
    attrs: AttributeSet?,
) : MultiSelectListPreference(context, attrs) {
    init {
        initialize { modules.getExternalModules(context) }

        onPreferenceChangeListener =
            OnPreferenceChangeListener { _, _ ->
                navigateToPreferencesScreen("pref.registry")
                sendBroadcastEvent(MODULES_LIST_CHANGED_EVENT)
                true
            }
    }

    override fun onAttached() {
        super.onAttached()

        findPreferenceInHierarchy<SwitchPreferenceCompat>(ACCESS_EXTERNAL_STORAGE_ENABLED)?.run {
            onPreferenceChangeListener =
                OnPreferenceChangeListener { _, new ->
                    initialize { modules.getExternalModules(context) { new.toString().toBoolean() } }
                    true
                }
        }
    }

    private fun initialize(files: () -> MutableMap<String, String>? = { null }) {
        val mutableMap =
            modules.getDefaultModules().toMutableMap().apply {
                files()?.let {
                    putAll(it)
                }
            }
        mutableMap.let {
            entries = it.values.toTypedArray()
            entryValues = it.keys.toTypedArray()
            setDefaultValue(it.keys)
        }
        Log.i(LOG_TAG, "Selected modules=${Prefs.getStringSet(PREF_MODULE_LIST, mutableSetOf())}")
    }
}
