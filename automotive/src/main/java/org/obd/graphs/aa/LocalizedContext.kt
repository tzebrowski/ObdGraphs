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
package org.obd.graphs.aa

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.car.app.CarContext
import org.obd.graphs.language.LanguageManager
import java.util.Locale
import java.util.WeakHashMap
import kotlin.collections.set

private val contextCache = WeakHashMap<CarContext, Pair<String, Context>>()

fun CarContext.getLocString(@StringRes resId: Int, vararg formatArgs: Any): String {
    val myLocaleCode = LanguageManager.getStoredLanguage(this)

    var cachedData = contextCache[this]

    if (cachedData == null || cachedData.first != myLocaleCode) {
        val config = Configuration(this.resources.configuration)
        config.setLocale(Locale(myLocaleCode))
        val newLocalizedContext = this.createConfigurationContext(config)

        cachedData = Pair(myLocaleCode, newLocalizedContext)
        contextCache[this] = cachedData
    }

    val localizedContext = cachedData.second

    return if (formatArgs.isEmpty()) {
        localizedContext.getString(resId)
    } else {
        localizedContext.getString(resId, *formatArgs)
    }
}
