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
package org.obd.graphs

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

private const val PREFS_FILE = "language_prefs"
private const val KEY_LANGUAGE = "language"
private const val KEY_SELECTED = "language_selected"

internal object LanguageManager {
    fun getLocalizedContext(context: Context): Context {
        val storedLang = getStoredLanguage(context)
        if (storedLang.isEmpty()) return context

        val locale = Locale.forLanguageTag(storedLang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun getStoredLanguage(context: Context): String =
        context
            .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "") ?: ""

    fun saveLanguage(
        context: Context,
        localeTag: String
    ) = context
        .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_LANGUAGE, localeTag)
        .putBoolean(KEY_SELECTED, true)
        .apply()

    fun isLanguageSelected(context: Context): Boolean =
        context
            .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_SELECTED, false)

    fun showLanguageSelectionDialog(
        activity: Activity,
        onComplete: (localeTag: String) -> Unit
    ) {
        val names = activity.resources.getStringArray(org.obd.graphs.commons.R.array.language_names)
        val codes = activity.resources.getStringArray(org.obd.graphs.commons.R.array.language_codes)

        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_language_selection, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.dialog_language_list)

        val dialog =
            AlertDialog
                .Builder(activity)
                .setView(dialogView)
                .setCancelable(false)
                .create()

        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter =
            LanguageAdapter(names) { index ->
                val localeTag = codes[index]
                saveLanguage(activity, localeTag)
                dialog.dismiss()
                onComplete(localeTag)
            }

        dialog.show()
    }
}
