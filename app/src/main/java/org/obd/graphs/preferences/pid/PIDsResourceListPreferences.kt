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
package org.obd.graphs.preferences.pid

import android.content.Context
import android.util.AttributeSet
import androidx.preference.CheckBoxPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.bl.datalogger.ACCESS_EXTERNAL_STORAGE_ENABLED
import org.obd.graphs.bl.datalogger.RESOURCE_LIST_CHANGED_EVENT
import org.obd.graphs.bl.datalogger.pidResources
import org.obd.graphs.sendBroadcastEvent


class PIDsResourceListPreferences(
    context: Context,
    attrs: AttributeSet?
) :
    MultiSelectListPreference(context, attrs) {

    init {
        initialize { pidResources.getExternalPidResources(context) }

        onPreferenceChangeListener =
            OnPreferenceChangeListener { _, _ ->
                navigateToPreferencesScreen("pref.registry")
                sendBroadcastEvent(RESOURCE_LIST_CHANGED_EVENT)
                true
            }
    }

    override fun onAttached() {
        super.onAttached()

        findPreferenceInHierarchy<CheckBoxPreference>(ACCESS_EXTERNAL_STORAGE_ENABLED)?.run {
            onPreferenceChangeListener = OnPreferenceChangeListener { _, new ->
                initialize { pidResources.getExternalPidResources(context) { new.toString().toBoolean() } }
                true
            }
        }
    }

    private fun initialize(files: () -> MutableMap<String, String>? = { null }) {
        val mutableMap = pidResources.getDefaultPidFiles().toMutableMap().apply {
            files()?.let {
                putAll(it)
            }
        }
        mutableMap.let {
            entries = it.values.toTypedArray()
            entryValues = it.keys.toTypedArray()
            setDefaultValue(it.keys)
        }
    }
}