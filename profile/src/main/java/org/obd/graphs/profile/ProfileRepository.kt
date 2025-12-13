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
package org.obd.graphs.profile

import android.content.SharedPreferences
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updatePreference

internal class ProfileRepository {
    fun getAll(): Map<String, *> = Prefs.all

    fun getString(
        key: String,
        default: String,
    ): String = Prefs.getString(key, default) ?: default

    fun update(
        key: String,
        value: Any?,
    )  = Prefs.edit().updatePreference(key, value)


    fun updateBatch(updates: Map<String, Any?>) {
        val editor = Prefs.edit()
        updates.forEach { (key, value) ->
            editor.updatePreference(key, value)
        }
        editor.apply()
    }

    fun remove(key: String) =  Prefs.edit().remove(key).apply()

    fun clear() = Prefs.edit().clear().apply()

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        Prefs.registerOnSharedPreferenceChangeListener(listener)

}
