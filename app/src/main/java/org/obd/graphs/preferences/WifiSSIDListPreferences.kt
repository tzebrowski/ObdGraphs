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
package org.obd.graphs.preferences

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.network
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.colorize
import java.util.*

private class SSID(val name: String)

class WifiSSIDListPreferences(
    context: Context,
    attrs: AttributeSet?
) :
    ListPreference(context, attrs) {
    init {
        setOnPreferenceChangeListener { _, _ ->
            navigateToPreferencesScreen("pref.adapter.connection")
            true
        }

        val entriesValues: MutableList<CharSequence> =
            LinkedList()
        val entries: MutableList<CharSequence> =
            LinkedList()

        getDeviceList {
            entries.add(it.name)
            entriesValues.add(it.name)
        }

        setEntries(entries.toTypedArray())
        entryValues = entriesValues.toTypedArray()
    }

    override fun getSummary(): CharSequence {
        return super.getSummary().toString().colorize(COLOR_PHILIPPINE_GREEN, Typeface.BOLD, 1.0f)
    }

    override fun getEntryValues(): Array<CharSequence> {
        val entriesValues: MutableList<CharSequence> =
            LinkedList()

        getDeviceList {
            entriesValues.add(it.name)
        }

        entryValues = entriesValues.toTypedArray()
        return super.getEntryValues()
    }

    override fun getEntries(): Array<CharSequence> {

        val entries: MutableList<CharSequence> =
            LinkedList()

        getDeviceList {
            entries.add(it.name)
        }

        setEntries(entries.toTypedArray())
        return super.getEntries()
    }

    private fun getDeviceList(handler: (device: SSID) -> Unit) {
        network.findWifiSSID().forEach {
            handler(SSID(it))
        }
    }
}
