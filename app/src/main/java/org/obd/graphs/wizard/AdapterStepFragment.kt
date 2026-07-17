/*
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
package org.obd.graphs.wizard

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.obd.graphs.R
import org.obd.graphs.preferences.PREFERENCE_CONNECTION_TYPE
import org.obd.graphs.preferences.PREFS_CONNECTION_TYPE_CHANGED_EVENT
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getString
import org.obd.graphs.sendBroadcastEvent

class AdapterStepFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        setPreferencesFromResource(R.xml.preferences, "pref.adapter.connection")
        registerConnectionTypeListener()
    }

    // Mirrors PreferencesFragment.registerConnectionTypeListener() - narrows the visible
    // sub-category to the currently selected connection type, since setPreferencesFromResource()
    // only inflates the XML subtree and doesn't carry over that fragment-specific behavior.
    private fun registerConnectionTypeListener() {
        val connectionType = findPreference<ListPreference>(PREFERENCE_CONNECTION_TYPE)
        val bluetoothCategory = findPreference<Preference>("$PREFERENCE_CONNECTION_TYPE.bluetooth")
        val wifiCategory = findPreference<Preference>("$PREFERENCE_CONNECTION_TYPE.wifi")
        val usbCategory = findPreference<Preference>("$PREFERENCE_CONNECTION_TYPE.usb")

        fun applyVisibility(type: String?) {
            bluetoothCategory?.isVisible = type == "bluetooth"
            wifiCategory?.isVisible = type == "wifi"
            usbCategory?.isVisible = type == "usb"
        }

        applyVisibility(Prefs.getString(PREFERENCE_CONNECTION_TYPE))

        connectionType?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                sendBroadcastEvent(PREFS_CONNECTION_TYPE_CHANGED_EVENT)
                applyVisibility(newValue as? String)
                true
            }
    }
}
