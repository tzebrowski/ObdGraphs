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
package org.obd.graphs.bl.datalogger.connectors

import java.util.UUID

/**
 * There is no standard OBD-over-BLE profile, so the service/characteristic UUIDs differ per
 * adapter. These cover the families in common use and are PROBED AFTER CONNECTING rather than
 * picked in advance - see [BleConnection].
 *
 * A profile whose write characteristic equals its notify characteristic is read/write on that one
 * characteristic, which is common on serial-bridge modules.
 */
data class BleProfile(
    val name: String,
    val service: UUID,
    val notifyCharacteristic: UUID,
    val writeCharacteristic: UUID
)

const val BLE_PROFILE_AUTO = "AUTO"
const val BLE_PROFILE_CUSTOM = "CUSTOM"

/**
 * Known profiles, most specific first. Ported from the sibling tuning-tools project, where each
 * entry was confirmed against real hardware.
 */
val BLE_PROFILES: List<BleProfile> =
    listOf(
        BleProfile(
            // The BLE models only. A vLinker MS is a CLASSIC Bluetooth adapter and will never
            // match this (or any) GATT profile.
            name = "Vgate iCar Pro BLE / vLinker MC+ (18F0)",
            service = uuid("000018f0-0000-1000-8000-00805f9b34fb"),
            notifyCharacteristic = uuid("00002af0-0000-1000-8000-00805f9b34fb"),
            writeCharacteristic = uuid("00002af1-0000-1000-8000-00805f9b34fb")
        ),
        BleProfile(
            name = "Generic ELM327 BLE (FFF0)",
            service = uuid("0000fff0-0000-1000-8000-00805f9b34fb"),
            notifyCharacteristic = uuid("0000fff1-0000-1000-8000-00805f9b34fb"),
            writeCharacteristic = uuid("0000fff2-0000-1000-8000-00805f9b34fb")
        ),
        BleProfile(
            // HM-10 and its clones: one characteristic for both directions. Confirmed against a
            // CCY STN-2120 4.0, which reported "ELM327 v1.4b" over this profile - STN-based
            // adapters matter here because the chip is also in the OBDLink CX/MX+ family.
            name = "HM-10 style / CCY STN-2120 (FFE0)",
            service = uuid("0000ffe0-0000-1000-8000-00805f9b34fb"),
            notifyCharacteristic = uuid("0000ffe1-0000-1000-8000-00805f9b34fb"),
            writeCharacteristic = uuid("0000ffe1-0000-1000-8000-00805f9b34fb")
        ),
        BleProfile(
            name = "Microchip / LELink (ISSC)",
            service = uuid("49535343-fe7d-4ae5-8fa9-9fafd205e455"),
            notifyCharacteristic = uuid("49535343-1e4d-4bd9-ba61-23c647249616"),
            writeCharacteristic = uuid("49535343-8841-43f4-a8d4-ecbe34729bb3")
        ),
        BleProfile(
            // Nordic UART Service. Not OBD-specific at all - it is the generic BLE serial bridge
            // a lot of modules ship with, so an adapter exposing it behaves like a serial ELM327.
            name = "Nordic UART Service",
            service = uuid("6e400001-b5a3-f393-e0a9-e50e24dcca9e"),
            notifyCharacteristic = uuid("6e400003-b5a3-f393-e0a9-e50e24dcca9e"),
            writeCharacteristic = uuid("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        ),
        BleProfile(
            name = "TI CC254x serial",
            service = uuid("0000ffe5-0000-1000-8000-00805f9b34fb"),
            notifyCharacteristic = uuid("0000ffe4-0000-1000-8000-00805f9b34fb"),
            writeCharacteristic = uuid("0000ffe9-0000-1000-8000-00805f9b34fb")
        ),
        BleProfile(
            name = "JDY / FEE7 module",
            service = uuid("0000fee7-0000-1000-8000-00805f9b34fb"),
            notifyCharacteristic = uuid("0000fec8-0000-1000-8000-00805f9b34fb"),
            writeCharacteristic = uuid("0000fec7-0000-1000-8000-00805f9b34fb")
        )
    )

/**
 * Candidates to probe for the given preference value: every known profile for [BLE_PROFILE_AUTO],
 * the user's three UUIDs for [BLE_PROFILE_CUSTOM], or the single pinned profile.
 *
 * An unparsable custom UUID yields no candidates, which surfaces as "adapter not set" rather than
 * a silent fallback to auto-probing something the user did not ask for.
 */
fun bleProfileCandidates(
    selected: String,
    customService: String = "",
    customNotify: String = "",
    customWrite: String = ""
): List<BleProfile> =
    when (selected) {
        BLE_PROFILE_AUTO -> BLE_PROFILES
        BLE_PROFILE_CUSTOM ->
            customProfile(customService, customNotify, customWrite)?.let { listOf(it) } ?: emptyList()
        else -> BLE_PROFILES.filter { it.name == selected }.ifEmpty { BLE_PROFILES }
    }

private fun customProfile(
    service: String,
    notify: String,
    write: String
): BleProfile? =
    try {
        BleProfile(
            name = "Custom",
            service = uuid(service),
            notifyCharacteristic = uuid(notify),
            // A blank write UUID means the module is read/write on one characteristic.
            writeCharacteristic = if (write.isBlank()) uuid(notify) else uuid(write)
        )
    } catch (_: IllegalArgumentException) {
        null
    }

private fun uuid(value: String): UUID = UUID.fromString(value.trim())
