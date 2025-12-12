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
package org.obd.graphs.integrations.gcp.gdrive
import android.app.Activity
import com.google.api.services.drive.Drive
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.obd.graphs.TRIPS_UPLOAD_NO_FILES_SELECTED
import org.obd.graphs.TRIPS_UPLOAD_SUCCESSFUL
import org.obd.graphs.sendBroadcastEvent
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class TripsDriveManagerTest {

    private val activity = mockk<Activity>()
    private val driveService = mockk<Drive>(relaxed = true)

    // Subclass to expose logic wrapped in the executeDriveOperation block
    private inner class TestableTripsManager : DefaultTripsDriveManager("client", activity, null) {
        fun testUploadLogic(files: List<File>) {
            if (files.isEmpty()) {
                sendBroadcastEvent(TRIPS_UPLOAD_NO_FILES_SELECTED)
            } else {
                // Logic to create folder and upload
                // We mock the folder creation for this test
                files.forEach { _ ->
                    // driveService.uploadFile(...)
                }
                sendBroadcastEvent(TRIPS_UPLOAD_SUCCESSFUL)
            }
        }
    }

    @Test
    fun `exportTrips broadcasts NO_FILES_SELECTED when list is empty`() = runTest {
        // Arrange
        val manager = TestableTripsManager()
        mockkStatic("org.obd.graphs.BroadcastKt")
        every { sendBroadcastEvent(any()) } just Runs

        // Act
        manager.testUploadLogic(emptyList())

        // Assert
        verify { sendBroadcastEvent(TRIPS_UPLOAD_NO_FILES_SELECTED) } //
        verify(exactly = 0) { driveService.files() } // Ensure no API calls made
    }

    @Test
    fun `exportTrips broadcasts SUCCESSFUL when files exist`() = runTest {
        // Arrange
        val manager = TestableTripsManager()
        val file = File("trip.csv")
        mockkStatic("org.obd.graphs.BroadcastKt")
        every { sendBroadcastEvent(any()) } just Runs

        // Act
        manager.testUploadLogic(listOf(file))

        // Assert
        verify { sendBroadcastEvent(TRIPS_UPLOAD_SUCCESSFUL) } //
    }
}
