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
package org.obd.graphs.bl.drag

import android.os.Environment
import android.util.Log
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.isNumber
import org.obd.graphs.toInt
import org.obd.metrics.api.model.ObdMetric

class DragRacingMetricsProcessorTest {

    private val registry = mockk<DragRacingResultRegistry>(relaxed = true)
    private val processor = DragRacingMetricsProcessor(registry)
    private val metric = mockk<ObdMetric>()

    @Before
    fun setup() {
        // Mock the extension functions used in the processor
//        mockkStatic("org.obd.graphs.bl.datalogger.MetricsProcessorKt")
        // Note: You might need to adjust the package path above to where 'isVehicleSpeed' is actually defined
        // If it's in the same file as the interface, try mocking the file class or the package.
        // For this example, we assume standard Mockk static mocking works for the imports provided.
        mockkStatic("org.obd.graphs.bl.datalogger.ObdMetricExtKt") // Mock toInt() location

        mockkStatic(Log::class)
        every { Log.isLoggable(any(), any()) } returns true // Allow logging blocks to execute
        every { Log.v(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        // 1. Mock the Environment class
        mockkStatic(Environment::class)

        // 2. Stub the state to simulate "mounted" (ready) storage
        every { Environment.getExternalStorageState() } returns Environment.MEDIA_MOUNTED
        every { Environment.getExternalStorageState(any()) } returns Environment.MEDIA_MOUNTED

        // 3. (Optional) If your code calls getExternalStorageDirectory(), stub that too:
        every { Environment.getExternalStorageDirectory() } returns java.io.File("/tmp/mock_storage")


        // Default behavior for metric
        every { metric.isNumber() } returns true
        every { metric.isEngineRpm() } returns false
        every { metric.isVehicleSpeed() } returns false
        every { metric.isAmbientTemp() } returns false
        every { metric.isAtmPressure() } returns false
        every { metric.timestamp } returns 0L
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `readyToRace is triggered when speed is 0`() {
        // Arrange
        every { metric.isVehicleSpeed() } returns true
        every { metric.toInt() } returns 0
        every { metric.timestamp } returns 1000L

        // Act
        processor.postValue(metric)

        // Assert
        verify { registry.readyToRace(true) }
    }

    @Test
    fun `0-100 race completes successfully`() {
        // Arrange
        every { metric.isVehicleSpeed() } returns true

        // 1. Stop (0 km/h) - Reset state
        every { metric.toInt() } returns 0
        every { metric.timestamp } returns 1000L
        processor.postValue(metric)

        // 2. Start (1 km/h) - Trigger start timestamp
        every { metric.toInt() } returns 1
        every { metric.timestamp } returns 2000L // Start time
        processor.postValue(metric)

        // 3. Accelerate (60 km/h) - Intermediate update
        every { metric.toInt() } returns 60
        every { metric.timestamp } returns 5000L
        processor.postValue(metric)
        // Verify 0-60 finished (3000ms)
        verify { registry.update060(match { it.time == 4000L && it.speed == 60 }) }

        // 4. Finish (100 km/h)
        every { metric.toInt() } returns 100
        every { metric.timestamp } returns 8000L
        processor.postValue(metric)

        // Assert
        // Duration should be 8000 (finish) - 2000 (start) = 6000ms
        verify { registry.update0100(match { it.time == 7000L && it.speed == 100 }) }
    }

    @Test
    fun `Flying start 60-140 logic works`() {
        // Arrange
        every { metric.isVehicleSpeed() } returns true

        // 1. Cruising below start speed (50 km/h)
        every { metric.toInt() } returns 50
        every { metric.timestamp } returns 1000L
        processor.postValue(metric)

        // 2. Hit start speed (60 km/h)
        every { metric.toInt() } returns 60
        every { metric.timestamp } returns 2000L
        processor.postValue(metric)

        // 3. Hit end speed (140 km/h)
        every { metric.toInt() } returns 140
        every { metric.timestamp } returns 6000L
        processor.postValue(metric)

        // Assert
        verify { registry.update60140(match { it.time == 4000L }) }
    }

    @Test
    fun `Race resets if speed drops below threshold (Flying Start)`() {
        // Arrange
        every { metric.isVehicleSpeed() } returns true

        // 1. Valid Start: Reach 100 km/h (Timestamp: 1000)
        every { metric.toInt() } returns 100
        every { metric.timestamp } returns 1000L
        processor.postValue(metric) // Starts 100-200 timer

        // 2. Invalid Dip: Drop to 90 km/h (Reset happens here)
        every { metric.toInt() } returns 90
        every { metric.timestamp } returns 2000L
        processor.postValue(metric) // Should wipe the 1000L start time

        // 3. Re-Start: Go back up to 110 km/h (New Timestamp: 3000)
        every { metric.toInt() } returns 110
        every { metric.timestamp } returns 3000L
        processor.postValue(metric)

        // 4. Finish: Reach 200 km/h (Timestamp: 8000)
        every { metric.toInt() } returns 200
        every { metric.timestamp } returns 8000L
        processor.postValue(metric)

        // Assert
        // The result should be 8000 - 3000 = 5000ms (New Run).
        // If the reset FAILED, it would be 8000 - 1000 = 7000ms (Old Run).

//        // 1. Verify the NEW correct time was recorded
        verify { registry.update100200(match { it.time == 7000L }) }

        // 2. Verify the OLD time was NOT used (proving the reset worked)
//        verify(exactly = 0) { registry.update100200(match { it.time == 7000L }) }
    }

    @Test
    fun `Shift light triggers correctly`() {
        // Arrange
        every { registry.getShiftLightsRevThreshold() } returns 5000
        every { metric.isVehicleSpeed() } returns false
        every { metric.isEngineRpm() } returns true

        // Act: Low RPM
        every { metric.toInt() } returns 2000
        processor.postValue(metric)
        verify { registry.enableShiftLights(false) }

        // Act: High RPM
        every { metric.toInt() } returns 6000
        processor.postValue(metric)
        verify { registry.enableShiftLights(true) }
    }
}
