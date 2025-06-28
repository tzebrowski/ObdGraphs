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

import org.obd.graphs.preferences.XmlPreference
import org.obd.graphs.modules

 data class Adapter(
    @XmlPreference("pref.adapter.id", "OBDII", String::class)
    var adapterId: String = "OBDII",
    @XmlPreference("pref.adapter.connection.type", "bluetooth", String::class)
    var connectionType: String = "bluetooth",
    @XmlPreference("pref.adapter.connection.timeout", "2000", Int::class)
    var connectionTimeout: Int = 2000,
    @XmlPreference("pref.adapter.stn.enabled", "false", Boolean::class)
    var stnExtensionsEnabled: Boolean = false,
    @XmlPreference("pref.adapter.query.individual.enabled", "false", Boolean::class)
    var individualQueryStrategyEnabled: Boolean = false,
    @XmlPreference("pref.adapter.connection.tcp.port", "35000", Int::class)
    var tcpPort: Int = 35000,
    @XmlPreference("pref.adapter.connection.tcp.ssid", "", String::class)
    var wifiSSID: String = "",
    @XmlPreference("pref.adapter.batch.size", "", Int::class)
    var otherModesBatchSize: Int? = null,
    @XmlPreference("pref.adapter.batch_01.size", "", Int::class)
    var mode01BatchSize: Int? = null,
    @XmlPreference("pref.adapter.batch.enabled", "true", Boolean::class)
    var batchEnabled: Boolean = true,
    @XmlPreference("pref.adapter.batch.strict_validation.enabled", "false", Boolean::class)
    var batchStrictValidationEnabled: Boolean = false,
    @XmlPreference("pref.adapter.connection.tcp.host", "192.168.0.10", String::class)
    var tcpHost: String = "192.168.0.10",
    @XmlPreference("pref.adapter.reconnect", "true", Boolean::class)
    var reconnectWhenError: Boolean = true,
    @XmlPreference("pref.adapter.command.freq", "6", Long::class)
    var commandFrequency: Long = 6,
    @XmlPreference("pref.adapter.init.delay", "500", Long::class)
    var initDelay: Long = 500,
    @XmlPreference("pref.adapter.init.delay_after_reset", "0", Long::class)
    var delayAfterReset: Long = 0,
    @XmlPreference("pref.adapter.cache.result.enabled", "false", Boolean::class)
    var resultsCacheEnabled: Boolean = false,
    @XmlPreference("pref.adapter.init.protocol", "AUTO", String::class)
    var initProtocol: String = "AUTO",
    @XmlPreference("pref.adapter.reconnect.max_retry", "0", Int::class)
    var maxReconnectNum: Int = 0,
    @XmlPreference("pref.adapter.init.fetchDeviceProperties", "true", Boolean::class)
    var vehicleMetadataReadingEnabled: Boolean = true,
    @XmlPreference("pref.adapter.init.fetchSupportedPids", "true", Boolean::class)
    var vehicleCapabilitiesReadingEnabled: Boolean = true,
    @XmlPreference("pref.adapter.init.fetchDTC", "false", Boolean::class)
    var vehicleDTCReadingEnabled: Boolean = false,
    @XmlPreference("pref.adapter.init.cleanDTC", "false", Boolean::class)
    var vehicleDTCCleaningEnabled: Boolean = false,
    @XmlPreference("pref.adapter.responseLength.enabled", "false", Boolean::class)
    var responseLengthEnabled: Boolean = false,
    @XmlPreference("pref.adapter.graceful_stop.enabled", "true", Boolean::class)
    var gracefulStop: Boolean = true,
    @XmlPreference("pref.adapter.adaptive.enabled", "false", Boolean::class)
    var adaptiveConnectionEnabled: Boolean = false,
)

data class DataLoggerSettings(
    var adapter: Adapter = Adapter(),
    @XmlPreference("pref.debug.logging.enabled", "false", Boolean::class)
    var debugLogging: Boolean = false,
    @XmlPreference("pref.drag_race.vehicle_speed.freq", "10", Long::class)
    var dragRacingCommandFrequency: Long = 10,
    @XmlPreference("pref.mode", "Generic mode", String::class)
    var mode: String = "Generic mode",
    @XmlPreference("pref.debug.generator.enabled", "false", Boolean::class)
    var generatorEnabled: Boolean = false,
    @XmlPreference("pref.pids.registry.list", "", Set::class)
    var resources: Set<String> = modules.getDefaultModules().keys,
    @XmlPreference("pref.debug.trip.save.connector_response", "false", Boolean::class)
    var dumpRawConnectorResponse: Boolean = false,
    @XmlPreference("pref.vehicle_settings.fuelTankSize", "58", Int::class)
    var fuelTankSize: Int = 58,
    @XmlPreference("pref.vehicle_settings.vehicle_status_panel_enabled", "false", Boolean::class)
    var vehicleStatusPanelEnabled: Boolean = false,
    @XmlPreference("pref.vehicle_settings.disconnect_when_off", "false", Boolean::class)
    var vehicleStatusDisconnectWhenOff: Boolean = false,
    @XmlPreference("pref.profile.2_0_GME_extension.enabled", "false", Boolean::class)
    var gmeExtensionsEnabled: Boolean = false,
)
