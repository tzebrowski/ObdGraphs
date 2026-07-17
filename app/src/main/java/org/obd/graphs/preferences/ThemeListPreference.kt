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
package org.obd.graphs.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import org.obd.graphs.theme.ThemeManager

// persistent="false" in the XML: theme is stored in ThemeManager's own SharedPreferences file (mirroring
// LanguagePreferences/LanguageManager), not the main Prefs, so the ListPreference's displayed value must
// be synced from there manually rather than relying on the framework's default persistence.
class ThemeListPreference(
    context: Context,
    attrs: AttributeSet?
) : ListPreference(context, attrs) {
    init {
        value = ThemeManager.getStoredTheme(context)

        setOnPreferenceChangeListener { _, newValue ->
            ThemeManager.saveTheme(context, newValue as String)
            true
        }
    }
}
