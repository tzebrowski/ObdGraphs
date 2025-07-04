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

import android.content.SharedPreferences
import android.util.Log
import androidx.core.text.isDigitsOnly
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField

private const val TAG = "AbstractPrefs"

data class CacheValue(
    val preference: XmlPreference,
    val field: KProperty1<*, *>,
    val obj: Any,
    val defaultValue: Any?,
)

interface PreferencesManager<T> {
    fun reload()

    fun instance(): T
}

abstract class AbstractPreferencesManager<T> : PreferencesManager<T> {
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
    protected val cache = mutableMapOf<String, CacheValue>()

    init {
        Prefs.registerOnSharedPreferenceChangeListener(strongReference)
    }

    protected fun update(
        key: String?,
        sharedPreferences: SharedPreferences?,
    ) {
        if (cache.containsKey(key)) {
            val cacheValue = cache[key]!!

            cacheValue.field.javaField?.isAccessible = true

            try {
                var newValue = sharedPreferences!!.all[key] ?: cacheValue.defaultValue

                if (!cacheValue.field.returnType.isMarkedNullable && newValue == null) {
                    Log.e(TAG, "Field is not marked nullable however, new one is null for $key ")
                } else {
                    newValue = normalizeNewValue(newValue, cacheValue.preference.type, key)

                    cacheValue.field.javaField?.set(cacheValue.obj, newValue)

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

    protected fun calculateDefaultValue(
        preference: XmlPreference,
        field: KProperty1<*, *>,
    ): Any? =
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

    private inline fun normalizeNewValue(
        value: Any?,
        type: KClass<*>,
        key: String?,
    ): Any? {
        var newValue = value

        if (newValue != null && newValue::class != type) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Types for $key differs ${newValue::class} != $type")
            }

            newValue =
                when (type) {
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
        return newValue
    }
}
