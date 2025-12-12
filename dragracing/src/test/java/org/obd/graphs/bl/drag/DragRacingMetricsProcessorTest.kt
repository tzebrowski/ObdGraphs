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

        mockkStatic("org.obd.graphs.bl.datalogger.ObdMetricExtKt")

        mockkStatic(Log::class)
        every { Log.isLoggable(any(), any()) } returns true // Allow logging blocks to execute
        every { Log.v(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        mockkStatic(Environment::class)

        every { Environment.getExternalStorageState() } returns Environment.MEDIA_MOUNTED
        every { Environment.getExternalStorageState(any()) } returns Environment.MEDIA_MOUNTED

        every { Environment.getExternalStorageDirectory() } returns java.io.File("/tmp/mock_storage")

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
    fun `readyToRace is not triggered`() {
        // Arrange
        every { metric.isVehicleSpeed() } returns true
        every { metric.toInt() } returns 10
        every { metric.timestamp } returns 1000L

        // Act
        processor.postValue(metric)

        // Assert
        verify { registry.readyToRace(false) }
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
    fun `0-60 race completes successfully`() {
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
        every { metric.timestamp } returns 6000L
        processor.postValue(metric)
        // Verify 0-60 finished (3000ms)
        verify { registry.update060(match { it.time == 5000L && it.speed == 60 }) }
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
    fun `0-160 race completes successfully`() {
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

        // 4.
        every { metric.toInt() } returns 100
        every { metric.timestamp } returns 8000L
        processor.postValue(metric)

        // 5. Finish (160 km/h)
        every { metric.toInt() } returns 160
        every { metric.timestamp } returns 12000L
        processor.postValue(metric)

        // Assert
        verify { registry.update0160(match { it.time == 11000L && it.speed == 160 }) }
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
    fun `Flying start 100-200 logic works`() {
        // Arrange
        every { metric.isVehicleSpeed() } returns true

        every { metric.toInt() } returns 100
        every { metric.timestamp } returns 1000L
        processor.postValue(metric)

        every { metric.toInt() } returns 160
        every { metric.timestamp } returns 4000L
        processor.postValue(metric)

        every { metric.toInt() } returns 200
        every { metric.timestamp } returns 12000L
        processor.postValue(metric)

        // Assert
        verify { registry.update100200(match { it.time == 11000L }) }
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

        // 1. Verify the NEW correct time was recorded
        verify { registry.update100200(match { it.time == 5000L }) }

        // 2. Verify the OLD time was NOT used (proving the reset worked)
        verify(exactly = 0) { registry.update100200(match { it.time == 7000L }) }
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
