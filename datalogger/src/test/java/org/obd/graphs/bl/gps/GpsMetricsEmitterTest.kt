/**
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
package org.obd.graphs.bl.gps

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.Permissions
import org.obd.graphs.bl.TestSetup
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.Reply
import org.obd.metrics.api.model.ReplyObserver
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.PidDefinitionRegistry
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GpsMetricsEmitterTest : TestSetup() {

    @MockK(relaxed = true)
    lateinit var mockReplyObserver: ReplyObserver<ObdMetric>

    @MockK
    lateinit var mockLocationManager: LocationManager

    @MockK
    lateinit var mockRegistry: PidDefinitionRegistry

    // Capture the listener passed to Android Location Manager
    private val locationListenerSlot = slot<LocationListener>()

    private lateinit var gpsEmitter: GpsMetricsEmitter

    @Before
    override fun setup() {
        super.setup()
        mockkObject(Permissions)
        mockkObject(dataLoggerSettings)
        mockkObject(dataLogger)


        // Mock System Service retrieval
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns mockLocationManager

        every { mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns true
        every {
            mockLocationManager.requestLocationUpdates(
                any<String>(),
                any<Long>(),
                any<Float>(),
                any<LocationListener>(),
                any<Looper>()
            )
        } just Runs
        every { mockLocationManager.removeUpdates(any<LocationListener>()) } just Runs

        // Mock LocationManager behaviors
        every { mockLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns true
        every { mockLocationManager.requestLocationUpdates(any<String>(), any<Long>(), any<Float>(), any<LocationListener>(), any<Looper>()) } just Runs
        every { mockLocationManager.removeUpdates(any<LocationListener>()) } just Runs

        // Mock Registry & Settings
        every { dataLogger.getPidDefinitionRegistry() } returns mockRegistry
        // Return dummy PID definition so the command initialization succeeds
        val dummyPid = mockk<PidDefinition>(relaxed = true) {
            every { id } returns 100L
            every { pid } returns "010C"
        }
        every { mockRegistry.findBy(any<Long>()) } returns dummyPid

        every { dataLoggerSettings.instance().adapter.gpsCollecetingEnabled } returns true
        every { Permissions.hasLocationPermissions(any()) } returns true
        every { Permissions.isLocationEnabled(any()) } returns true

        gpsEmitter = GpsMetricsEmitter()
        gpsEmitter.init(mockReplyObserver as ReplyObserver<Reply<*>>)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onRunning should start location updates if permissions granted`() {
        // Act
        gpsEmitter.onRunning(null)

        // Assert
        verify {
            mockLocationManager.requestLocationUpdates(
                eq(LocationManager.GPS_PROVIDER),
                any(),
                any(),
                capture(locationListenerSlot),
                any() // Looper
            )
        }
    }

    @Test
    fun `should IGNORE invalid (0,0) coordinates`() {
        // Arrange
        gpsEmitter.onRunning(null)
        verify { mockLocationManager.requestLocationUpdates(any<String>(), any(), any(), capture(locationListenerSlot), any()) }

        val listener = locationListenerSlot.captured
        val invalidLocation = mockk<Location>(relaxed = true)
        every { invalidLocation.latitude } returns 0.0
        every { invalidLocation.longitude } returns 0.0

        // Act
        listener.onLocationChanged(invalidLocation)

        // Assert
        verify(exactly = 0) { mockReplyObserver.onNext(any()) }
    }

    @Test
    fun `should EMIT metrics for valid coordinates`() {
        // Arrange
        gpsEmitter.onRunning(null)
        verify { mockLocationManager.requestLocationUpdates(any<String>(), any(), any(), capture(locationListenerSlot), any()) }

        val listener = locationListenerSlot.captured
        val validLocation = mockk<Location>(relaxed = true)
        every { validLocation.latitude } returns 52.2297
        every { validLocation.longitude } returns 21.0122
        every { validLocation.altitude } returns 100.0
        every { validLocation.bearing } returns 10f
        every { validLocation.accuracy } returns 5f

        // Act
        listener.onLocationChanged(validLocation)

        // Assert: Expect 4 metrics (Latitude, Longitude, Altitude, Location composite)
        verify(exactly = 4) { mockReplyObserver.onNext(any()) }
    }

    @Test
    fun `should stop location updates onStopped`() {
        // Arrange
        gpsEmitter.onRunning(null)
        verify { mockLocationManager.requestLocationUpdates(any<String>(), any(), any(), capture(locationListenerSlot), any()) }
        val listener = locationListenerSlot.captured

        // Act
        gpsEmitter.onStopped()

        // Assert
        verify { mockLocationManager.removeUpdates(listener) }
    }
}