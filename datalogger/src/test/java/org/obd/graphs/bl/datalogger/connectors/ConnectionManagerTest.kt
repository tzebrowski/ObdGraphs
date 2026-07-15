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

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.Network
import org.obd.graphs.bl.TestSetup
import org.obd.graphs.bl.datalogger.Adapter
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ADAPTER_NOT_SET_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_WIFI_INCORRECT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_WIFI_NOT_CONNECTED
import org.obd.graphs.bl.datalogger.DataLoggerSettings
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.metrics.api.model.Adjustments
import org.obd.metrics.api.model.Init
import org.obd.metrics.api.model.Query
import org.obd.metrics.pid.PidDefinitionRegistry
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ConnectionManagerTest : TestSetup() {

    private val registry = mockk<PidDefinitionRegistry>(relaxed = true)
    private val query = mockk<Query>(relaxed = true)
    private val adjustments = mockk<Adjustments>(relaxed = true)
    private val init = mockk<Init>(relaxed = true)

    @Before
    override fun setup() {
        super.setup()
        mockkObject(dataLoggerSettings)
        mockkObject(Network)
        mockkStatic("org.obd.graphs.BroadcastKt")
        every { org.obd.graphs.sendBroadcastEvent(any<String>()) } returns Unit
    }

    @After
    fun tearDown() = unmockkAll()

    private fun settingsWith(connectionType: String, adapter: Adapter = Adapter()) {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(adapter = adapter.copy(connectionType = connectionType))
    }

    @Test
    fun `obtain returns null for an unknown connection type`() {
        settingsWith("none")

        val result = ConnectionManager.obtain(registry, query, adjustments, init)

        assertNull(result)
    }

    @Test
    fun `bluetooth connection is rejected and broadcasts when device address is empty`() {
        settingsWith("bluetooth", Adapter(deviceAddress = ""))

        val result = ConnectionManager.obtain(registry, query, adjustments, init)

        assertNull(result)
        verify { org.obd.graphs.sendBroadcastEvent(DATA_LOGGER_ADAPTER_NOT_SET_EVENT) }
    }

    @Test
    fun `bluetooth connection is rejected and broadcasts when the adapter cannot be found`() {
        settingsWith("bluetooth", Adapter(deviceAddress = "AA:BB:CC:DD:EE:FF"))
        every { Network.findBluetoothAdapterByName("AA:BB:CC:DD:EE:FF") } returns null

        val result = ConnectionManager.obtain(registry, query, adjustments, init)

        assertNull(result)
        verify { org.obd.graphs.sendBroadcastEvent(DATA_LOGGER_ADAPTER_NOT_SET_EVENT) }
    }

    @Test
    fun `wifi connection is rejected and broadcasts when not connected to any wifi network`() {
        settingsWith("wifi", Adapter(wifiSSID = "MyHomeWifi"))
        every { Network.currentSSID } returns null

        val result = ConnectionManager.obtain(registry, query, adjustments, init)

        assertNull(result)
        verify { org.obd.graphs.sendBroadcastEvent(DATA_LOGGER_WIFI_NOT_CONNECTED) }
    }

    @Test
    fun `wifi connection is rejected and broadcasts when connected to the wrong ssid`() {
        settingsWith("wifi", Adapter(wifiSSID = "MyHomeWifi"))
        every { Network.currentSSID } returns "SomeOtherWifi"

        val result = ConnectionManager.obtain(registry, query, adjustments, init)

        assertNull(result)
        verify { org.obd.graphs.sendBroadcastEvent(DATA_LOGGER_WIFI_INCORRECT) }
    }

    @Test
    fun `usb connection type builds a usb connection from preferences`() {
        settingsWith("usb")

        val result = ConnectionManager.obtain(registry, query, adjustments, init)

        assertNotNull(result)
    }
}
