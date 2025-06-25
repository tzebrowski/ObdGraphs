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
import org.obd.graphs.PREF_MODULE_LIST
import org.obd.graphs.modules
import org.obd.graphs.preferences.Prefs
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

private const val TAG = "DataLoggerSettings"

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Preference(val key: String, val defaultValue: String, val type: KClass<*>)

data class Adapter(
    @Preference("pref.adapter.id", "OBDII", String::class)
    var adapterId: String = "OBDII",

    @Preference("pref.adapter.connection.type", "bluetooth", String::class)
    var connectionType: String = "bluetooth",

    @Preference("pref.adapter.connection.timeout", "2000", Int::class)
    var connectionTimeout: Int = 2000,

    @Preference("pref.adapter.stn.enabled", "false", Boolean::class)
    var stnExtensionsEnabled: Boolean = false,

    @Preference("pref.adapter.query.individual.enabled", "false", Boolean::class)
    var individualQueryStrategyEnabled: Boolean = false,

    @Preference("pref.adapter.connection.tcp.port", "35000", Int::class)
    var tcpPort: Int = 35000,

    @Preference("pref.adapter.connection.tcp.ssid", "", String::class)
    var wifiSSID: String = "",

    @Preference("pref.adapter.batch.size", "", Int::class)
    var otherModesBatchSize: Int? = null,

    @Preference("pref.adapter.batch_01.size", "", Int::class)
    var mode01BatchSize: Int? = null,

    @Preference("pref.adapter.batch.enabled", "true", Boolean::class)
    var batchEnabled: Boolean = true,

    @Preference("pref.adapter.batch.strict_validation.enabled", "false", Boolean::class)
    var batchStrictValidationEnabled: Boolean = false,

    @Preference("pref.adapter.connection.tcp.host", "192.168.0.10", String::class)
    var tcpHost: String = "192.168.0.10",

    @Preference("pref.adapter.reconnect", "true", Boolean::class)
    var reconnectWhenError: Boolean = true,

    @Preference("pref.adapter.command.freq", "6", Long::class)
    var commandFrequency: Long = 6,

    @Preference("pref.adapter.init.delay", "500", Long::class)
    var initDelay: Long = 500,

    @Preference("pref.adapter.init.delay_after_reset", "0", Long::class)
    var delayAfterReset: Long = 0,

    @Preference("pref.adapter.cache.result.enabled", "false", Boolean::class)
    var resultsCacheEnabled: Boolean = false,

    @Preference("pref.adapter.init.protocol", "AUTO", String::class)
    var initProtocol: String = "AUTO",

    @Preference("pref.adapter.reconnect.max_retry", "0", Int::class)
    var maxReconnectNum: Int = 0,

    @Preference("pref.adapter.init.fetchDeviceProperties", "true", Boolean::class)
    var vehicleMetadataReadingEnabled: Boolean = true,

    @Preference("pref.adapter.init.fetchSupportedPids", "true", Boolean::class)
    var vehicleCapabilitiesReadingEnabled: Boolean = true,

    @Preference("pref.adapter.init.fetchDTC", "false", Boolean::class)
    var vehicleDTCReadingEnabled: Boolean = false,

    @Preference("pref.adapter.init.cleanDTC", "false", Boolean::class)
    var vehicleDTCCleaningEnabled: Boolean = false,

    @Preference("pref.adapter.responseLength.enabled", "false", Boolean::class)
    var responseLengthEnabled: Boolean = false,

    @Preference("pref.adapter.graceful_stop.enabled", "true", Boolean::class)
    var gracefulStop: Boolean = true,

    @Preference("pref.adapter.adaptive.enabled", "false", Boolean::class)
    var adaptiveConnectionEnabled: Boolean = false,
)

data class DataLoggerSettings(
    var adapter: Adapter = Adapter(),

    @Preference("pref.debug.logging.enabled", "false", Boolean::class)
    var debugLogging: Boolean = false,

    @Preference("pref.drag_race.vehicle_speed.freq", "10", Long::class)
    var dragRacingCommandFrequency: Long = 10,

    @Preference("pref.mode", "Generic mode", String::class)
    var mode: String = "Generic mode",

    @Preference("pref.debug.generator.enabled", "false", Boolean::class)
    var generatorEnabled: Boolean = false,

    @Preference("pref.pids.registry.list", "", Set::class)
    var resources: Set<String> = modules.getDefaultModules().keys,

    @Preference("pref.debug.trip.save.connector_response", "false", Boolean::class)
    var dumpRawConnectorResponse: Boolean = false,

    @Preference("pref.vehicle_settings.fuelTankSize", "58", Int::class)
    var fuelTankSize: Int = 58,

    @Preference("pref.vehicle_settings.vehicle_status_panel_enabled", "false", Boolean::class)
    var vehicleStatusPanelEnabled: Boolean = false,

    @Preference("pref.vehicle_settings.disconnect_when_off", "false", Boolean::class)
    var vehicleStatusDisconnectWhenOff: Boolean = false,

    @Preference("pref.profile.2_0_GME_extension.enabled", "false", Boolean::class)
    var gmeExtensionsEnabled: Boolean = false,
)

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

    init {
        Prefs.registerOnSharedPreferenceChangeListener(strongReference)
        reload()
    }

    override fun instance(): DataLoggerSettings = instance

    override fun reload() {
        update("pref.adapter.connection.type", Prefs)
        update("pref.adapter.connection.timeout", Prefs)
        update("pref.adapter.stn.enabled", Prefs)
        update("pref.adapter.query.individual.enabled", Prefs)
        update("pref.adapter.connection.tcp.host", Prefs)
        update("pref.adapter.connection.tcp.port", Prefs)
        update("pref.adapter.connection.tcp.ssid", Prefs)
        update("pref.adapter.batch.size", Prefs)
        update("pref.adapter.batch_01.size", Prefs)
        update("pref.adapter.batch.enabled", Prefs)
        update("pref.adapter.batch.strict_validation.enabled", Prefs)
        update("pref.adapter.reconnect", Prefs)
        update("pref.adapter.id", Prefs)
        update("pref.adapter.command.freq", Prefs)
        update("pref.adapter.init.delay", Prefs)
        update("pref.adapter.init.delay_after_reset", Prefs)
        update("pref.adapter.cache.result.enabled", Prefs)
        update("pref.adapter.init.protocol", Prefs)
        update("pref.adapter.reconnect.max_retry", Prefs)
        update("pref.adapter.init.fetchSupportedPids", Prefs)
        update("pref.adapter.init.fetchDTC", Prefs)
        update("pref.adapter.init.cleanDTC", Prefs)
        update("pref.adapter.responseLength.enabled", Prefs)
        update("pref.adapter.init.fetchDeviceProperties", Prefs)
        update("pref.adapter.graceful_stop.enabled", Prefs)
        update("pref.adapter.adaptive.enabled", Prefs)
        update("pref.debug.logging.enabled", Prefs)
        update("pref.drag_race.vehicle_speed.freq", Prefs)
        update("pref.mode", Prefs)
        update("pref.debug.generator.enabled", Prefs)
        update("pref.debug.trip.save.connector_response", Prefs)
        update(PREF_MODULE_LIST, Prefs)
        update("pref.vehicle_settings.fuelTankSize", Prefs)
        update("pref.vehicle_settings.vehicle_status_panel_enabled", Prefs)
        update("pref.vehicle_settings.disconnect_when_off", Prefs)
        update("pref.profile.2_0_GME_extension.enabled", Prefs)
    }

    private fun update(key: String?, sharedPreferences: SharedPreferences?) {
        var obj: Any = instance
        var field: KProperty1<*, *>? = instance::class.declaredMemberProperties.find {
            (it.javaField?.annotations?.find { an -> an is Preference } as Preference?)?.key.equals(key)
        }
        if (field == null) {
            field = instance.adapter::class.declaredMemberProperties.find {
                (it.javaField?.annotations?.find { an -> an is Preference } as Preference?)?.key.equals(key)
            }
            obj = instance.adapter
        }
        if (field == null) {
            if (Log.isLoggable(TAG,Log.DEBUG)) {
                Log.d(TAG, "Did not find mapping for $key")
            }
        } else {
            val preference = field.javaField?.getAnnotation(Preference::class.java)
            if (preference == null) {
                if (Log.isLoggable(TAG,Log.DEBUG)) {
                   Log.d(TAG, " Did not find Preference annotation for $key")
                }
            } else {
                field.javaField?.isAccessible = true

                val default: Any? = if (preference.type == String::class) {
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

                            newValue = when (preference.type) {
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
                        if (Log.isLoggable(TAG, Log.INFO)){
                            Log.i(TAG, "Preference $key is updated with new value=$newValue")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update property $key", e)
                }
            }
        }
    }
}

val dataLoggerSettings: SettingsManager by lazy { DataLoggerSettingsManager() }
