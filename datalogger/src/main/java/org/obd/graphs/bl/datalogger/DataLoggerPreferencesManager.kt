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
package org.obd.graphs.bl.datalogger

import org.obd.graphs.preferences.AbstractPreferencesManager
import org.obd.graphs.preferences.CacheValue
import org.obd.graphs.preferences.PreferencesManager
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.XmlPreference
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

internal class DataLoggerPreferencesManager : AbstractPreferencesManager<DataLoggerSettings>() {
    private val instance: DataLoggerSettings = DataLoggerSettings()

    init {
        reload()
    }

    override fun instance(): DataLoggerSettings = instance

    override fun reload() {
        instance::class.declaredMemberProperties.forEach { field ->
            val preference = field.javaField?.annotations?.find { an -> an is XmlPreference } as XmlPreference?
            preference?.let {
                cache[preference.key] = CacheValue(preference, field, instance, calculateDefaultValue(preference, field))
                update(preference.key, Prefs)
            }
        }

        instance.adapter::class.declaredMemberProperties.forEach { field ->
            val preference = field.javaField?.annotations?.find { an -> an is XmlPreference } as XmlPreference?
            preference?.let {
                cache[preference.key] = CacheValue(preference, field, instance.adapter, calculateDefaultValue(preference, field))
                update(preference.key, Prefs)
            }
        }
    }
}

val dataLoggerSettings: PreferencesManager<DataLoggerSettings> by lazy { DataLoggerPreferencesManager() }
