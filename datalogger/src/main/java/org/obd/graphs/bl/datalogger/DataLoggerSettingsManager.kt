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

import android.content.SharedPreferences
import android.util.Log
import androidx.core.text.isDigitsOnly
import org.obd.graphs.preferences.Prefs
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

private const val TAG = "DataLoggerSettings"

interface SettingsManager {
    fun reload()

    fun instance(): DataLoggerSettings
}

internal class DataLoggerSettingsManager : SettingsManager {
    private inner class SharedPreferenceChangeListener : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?,
        ) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Key to update $key")
            }

            update(key, sharedPreferences)
        }
    }

    private var strongReference: SharedPreferenceChangeListener = SharedPreferenceChangeListener()
    private var instance: DataLoggerSettings = DataLoggerSettings()
    private val cache = mutableMapOf<String, Triple<Preference, KProperty1<*, *>, Any>>()

    init {
        Prefs.registerOnSharedPreferenceChangeListener(strongReference)
        reload()
    }

    override fun instance(): DataLoggerSettings = instance

    override fun reload() {
        instance::class.declaredMemberProperties.forEach { field ->
            val preference = field.javaField?.annotations?.find { an -> an is Preference } as Preference?
            preference?.let {
                cache[preference.key] = Triple(preference, field, instance)
                update(preference.key, Prefs)
            }
        }

        instance.adapter::class.declaredMemberProperties.forEach { field ->
            val preference = field.javaField?.annotations?.find { an -> an is Preference } as Preference?
            preference?.let {
                cache[preference.key] = Triple(preference, field, instance.adapter)
                update(preference.key, Prefs)
            }
        }
    }

    private fun update(
        key: String?,
        sharedPreferences: SharedPreferences?,
    ) {
        if (cache.containsKey(key)) {
            val (preference, field, obj) = cache[key]!!

            field.javaField?.isAccessible = true

            val default: Any? =
                if (preference.type == String::class) {
                    preference.defaultValue
                } else if (preference.type == Int::class) {
                    if (preference.defaultValue.isNotEmpty() && preference.defaultValue.isDigitsOnly()) {
                        preference.defaultValue.toInt()
                    } else {
                        preference.defaultValue
                    }
                } else if (preference.type == Boolean::class) {
                    preference.defaultValue.toBoolean()
                } else if (preference.type == Long::class) {
                    if (preference.defaultValue.isNotEmpty() && preference.defaultValue.isDigitsOnly()) {
                        preference.defaultValue.toLong()
                    } else {
                        preference.defaultValue
                    }
                } else if (preference.type == Set::class) {
                    null
                } else if (field.returnType.isMarkedNullable) {
                    null
                } else {
                    null
                }

            try {
                var newValue = sharedPreferences!!.all[key] ?: default

                if (!field.returnType.isMarkedNullable && newValue == null) {
                    Log.e(TAG, "Field is not marked nullable however, new one is null for $key ")
                } else {
                    if (newValue != null && newValue::class != preference.type) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "Types for $key differs ${newValue::class} != ${preference.type}")
                        }

                        newValue =
                            when (preference.type) {
                                Int::class -> {
                                    if (newValue.toString().isNotEmpty() && newValue.toString().isDigitsOnly()) {
                                        newValue.toString().toInt()
                                    } else {
                                        null
                                    }
                                }

                                Long::class -> {
                                    if (newValue.toString().isNotEmpty() && newValue.toString().isDigitsOnly()) {
                                        newValue.toString().toLong()
                                    } else {
                                        null
                                    }
                                }

                                Boolean::class -> {
                                    newValue.toString().toBoolean()
                                }

                                else -> {
                                    newValue
                                }
                            }
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            if (newValue != null) {
                                Log.d(TAG, "New type for $key is set to ${newValue::class}")
                            }
                        }
                    }
                    field.javaField?.set(obj, newValue)
                    if (Log.isLoggable(TAG, Log.INFO)) {
                        Log.i(TAG, "Preference $key is updated with new value=$newValue")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update property $key", e)
            }
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Did not find mapping for $key")
            }
        }
    }
}

val dataLoggerSettings: SettingsManager by lazy { DataLoggerSettingsManager() }
