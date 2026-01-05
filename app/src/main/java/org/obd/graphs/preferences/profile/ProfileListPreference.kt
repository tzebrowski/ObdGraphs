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
package org.obd.graphs.preferences.profile

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.preference.ListPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.profile.PROFILES_PREF
import org.obd.graphs.profile.profile
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.colorize

class ProfileListPreference(
    context: Context,
    attrs: AttributeSet?,
) : ListPreference(context, attrs) {
    init {

        profile
            .getAvailableProfiles()
            .let {
                entries = it.values.toTypedArray()
                entryValues = it.keys.toTypedArray()
                if (it.keys.isNotEmpty()) {
                    setDefaultValue(it.keys.first())
                }
            }

        onPreferenceChangeListener =
            OnPreferenceChangeListener { _, newValue ->
                profile.saveCurrentProfile()
                profile.loadProfile(newValue.toString())
                navigateToPreferencesScreen(PROFILES_PREF)
                true
            }
    }

    override fun getSummary(): CharSequence = super.getSummary().toString().colorize(COLOR_PHILIPPINE_GREEN, Typeface.BOLD, 1.0f)
}
