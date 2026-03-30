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

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import org.obd.graphs.language.LanguageManager
import java.util.Locale

class LocalizedStringProvider(private val context: Context) {

    private var cachedContext: Context? = null
    private var cachedLocaleCode: String? = null

    private fun getSmartContext(): Context {
        val currentLocaleCode = LanguageManager.getStoredLanguage(context)

        if (cachedContext == null || cachedLocaleCode != currentLocaleCode) {
            val config = Configuration(context.resources.configuration)
            config.setLocale(Locale(currentLocaleCode))

            cachedContext = context.createConfigurationContext(config)
            cachedLocaleCode = currentLocaleCode
        }

        return cachedContext!!
    }

    fun getString(@StringRes resId: Int): String {
        return getSmartContext().getString(resId)
    }

    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String {
        return getSmartContext().getString(resId, *formatArgs)
    }
}
