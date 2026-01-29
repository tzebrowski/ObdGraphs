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
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.location.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.Permissions
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.getContext
import org.obd.graphs.preferences.Prefs
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.Reply
import org.obd.metrics.api.model.ReplyObserver
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.PidDefinitionRegistry
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GpsMetricsEmitterTest {

    private lateinit var context: ContextWrapper
    protected val sharedPrefs = mockk<SharedPreferences>(relaxed = true)
    protected val editor = mockk<SharedPreferences.Editor>(relaxed = true)

    @MockK(relaxed = true)
    lateinit var mockReplyObserver: ReplyObserver<ObdMetric>

    @MockK
    lateinit var mockFusedLocationClient: FusedLocationProviderClient

    @MockK
    lateinit var mockRegistry: PidDefinitionRegistry

    // Capture the callback passed to Android Location Services
    private val locationCallbackSlot = slot<LocationCallback>()

    private lateinit var gpsEmitter: GpsMetricsEmitter

    private fun mockContext() {
        context = ApplicationProvider.getApplicationContext()
        mockkStatic(::getContext)
        every { getContext()  } returns context
    }

    private fun mockLog() {
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.isLoggable(any(), any()) } returns false
    }


    private fun mockPrefs() {
        mockkStatic("org.obd.graphs.preferences.PreferencesKt")
        every { Prefs } returns sharedPrefs
        every { sharedPrefs.edit() } returns editor
        every { sharedPrefs.all } returns emptyMap()
        every { sharedPrefs.getString(any(), any()) } answers { secondArg() }
        every { sharedPrefs.getBoolean(any(), any()) } returns false // Default to false for installation check
        every { sharedPrefs.registerOnSharedPreferenceChangeListener(any()) } just Runs
    }

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockContext()
        mockLog()
        mockPrefs()

        // 1. Mock Static Dependencies
        mockkObject(Permissions)
        mockkObject(dataLoggerSettings)
        mockkObject(dataLogger)

        // Mock LocationServices static factory
        mockkStatic(LocationServices::class)
        every { LocationServices.getFusedLocationProviderClient(any<Context>()) } returns mockFusedLocationClient

        // 2. Mock Registry & Settings
        every { dataLogger.getPidDefinitionRegistry() } returns mockRegistry
        // Return dummy PID definition so the command initialization succeeds
        val dummyPid = mockk<PidDefinition>(relaxed = true) {
            every { id } returns 100L
            every { pid } returns "010C"
        }
        // Use any<Long>() to match the ID type specifically
        every { mockRegistry.findBy(any<Long>()) } returns dummyPid

        every { dataLoggerSettings.instance().adapter.gpsCollecetingEnabled } returns true
        every { Permissions.hasLocationPermissions(any()) } returns true

        mockkStatic(LocationServices::class)
        every { LocationServices.getFusedLocationProviderClient(any<Context>()) } returns mockFusedLocationClient

        every { mockFusedLocationClient.requestLocationUpdates(any(), any<LocationCallback>(), any()) } returns mockk()

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
            mockFusedLocationClient.requestLocationUpdates(
                any(), // LocationRequest
                capture(locationCallbackSlot),
                any()  // Looper
            )
        }
    }

    @Test
    fun `should IGNORE invalid (0,0) coordinates`() {
        // Arrange
        gpsEmitter.onRunning(null)
        // Ensure verify passed before proceeding
        verify { mockFusedLocationClient.requestLocationUpdates(any(), capture(locationCallbackSlot), any()) }

        val callback = locationCallbackSlot.captured

        val invalidLocation = mockk<Location>(relaxed = true)
        every { invalidLocation.latitude } returns 0.0
        every { invalidLocation.longitude } returns 0.0

        val result = LocationResult.create(listOf(invalidLocation))

        // Act
        callback.onLocationResult(result)

        // Assert
        verify(exactly = 0) { mockReplyObserver.onNext(any()) }
    }

    @Test
    fun `should EMIT metrics for valid coordinates`() {
        // Arrange
        gpsEmitter.onRunning(null)

        // Ensure capture
        verify { mockFusedLocationClient.requestLocationUpdates(any(), capture(locationCallbackSlot), any()) }
        val callback = locationCallbackSlot.captured

        // Act: Send Valid Location
        val validLocation = mockk<Location>(relaxed = true)
        every { validLocation.latitude } returns 52.2297
        every { validLocation.longitude } returns 21.0122
        every { validLocation.altitude } returns 100.0
        val result = LocationResult.create(listOf(validLocation))

        callback.onLocationResult(result)

        // Assert: 3 metrics emitted
        verify(exactly = 3) { mockReplyObserver.onNext(any()) }
    }

    @Test
    fun `should stop location updates onStopped`() {
        // Arrange
        gpsEmitter.onRunning(null)

        // Verify start happened FIRST to ensure the slot is filled
        verify {
            mockFusedLocationClient.requestLocationUpdates(
                any(),
                capture(locationCallbackSlot),
                any()
            )
        }

        // Now it is safe to access .captured
        val callback = locationCallbackSlot.captured

        // Act
        gpsEmitter.onStopped()

        // Assert
        verify { mockFusedLocationClient.removeLocationUpdates(callback) }
    }
}
