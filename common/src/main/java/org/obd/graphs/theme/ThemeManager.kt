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
package org.obd.graphs.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

private const val PREFS_FILE = "theme_prefs"
private const val KEY_THEME = "theme"

const val THEME_SYSTEM = "system"
const val THEME_LIGHT = "light"
const val THEME_DARK = "dark"

object ThemeManager {
    fun getStoredTheme(context: Context): String =
        context
            .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

    // AppCompatDelegate.setDefaultNightMode() is process-global and automatically recreates every
    // currently-resumed AppCompatActivity, so there's no per-Activity attachBaseContext equivalent needed.
    fun saveTheme(
        context: Context,
        theme: String
    ) {
        context
            .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme)
            .apply()
        applyTheme(theme)
    }

    fun applyStoredTheme(context: Context) {
        applyTheme(getStoredTheme(context))
    }

    private fun applyTheme(theme: String) {
        AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
