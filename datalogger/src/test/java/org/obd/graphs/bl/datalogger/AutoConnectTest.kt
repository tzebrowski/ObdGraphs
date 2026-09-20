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
package org.obd.graphs.bl.datalogger

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.Network
import org.obd.graphs.bl.TestSetup
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [AutoConnect]'s debounce is static state that Robolectric does NOT reliably reset between
 * methods sharing an SDK config, so every test here pins the window to 0 rather than depending on
 * execution order.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [29])
class AutoConnectTest : TestSetup() {

    @Before
    override fun setup() {
        super.setup()
        mockkObject(dataLoggerSettings)
        mockkObject(Network)
        every { Network.startBondedDeviceMonitor(any(), any(), any()) } just Runs
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `schedule starts a background scan when enabled with a valid mac address on a supported SDK`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(adapter = Adapter(deviceAddress = "aa:bb:cc:dd:ee:ff"))

        AutoConnect.schedule(context, autoConnectEnabled = true, debounceWindowMs = 0)

        verify { Network.startBondedDeviceMonitor(context, "AA:BB:CC:DD:EE:FF", any()) }
    }

    // Each Bluetooth transport stores its MAC under its own key, so watching the Classic address
    // while BLE is selected would monitor a device the user is not connecting to.
    @Test
    fun `schedule watches the ble address when the ble connection type is selected`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(
                adapter =
                Adapter(
                    connectionType = "ble",
                    deviceAddress = "11:22:33:44:55:66",
                    bleDeviceAddress = "aa:bb:cc:dd:ee:ff"
                )
            )

        AutoConnect.schedule(context, autoConnectEnabled = true, debounceWindowMs = 0)

        verify { Network.startBondedDeviceMonitor(context, "AA:BB:CC:DD:EE:FF", any()) }
    }

    @Test
    fun `schedule does nothing when auto connect is disabled`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(adapter = Adapter(deviceAddress = "aa:bb:cc:dd:ee:ff"))

        AutoConnect.schedule(context, autoConnectEnabled = false, debounceWindowMs = 0)

        verify(exactly = 0) { Network.startBondedDeviceMonitor(any(), any(), any()) }
    }

    @Test
    fun `schedule does nothing when the device address is empty`() {
        every { dataLoggerSettings.instance() } returns DataLoggerSettings(adapter = Adapter(deviceAddress = ""))

        AutoConnect.schedule(context, autoConnectEnabled = true, debounceWindowMs = 0)

        verify(exactly = 0) { Network.startBondedDeviceMonitor(any(), any(), any()) }
    }

    @Test
    @Config(manifest = Config.NONE, sdk = [28])
    fun `schedule does nothing on Android versions below Q`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(adapter = Adapter(deviceAddress = "aa:bb:cc:dd:ee:ff"))

        AutoConnect.schedule(context, autoConnectEnabled = true, debounceWindowMs = 0)

        verify(exactly = 0) { Network.startBondedDeviceMonitor(any(), any(), any()) }
    }
}
