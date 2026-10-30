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
package org.obd.graphs.integrations.gcp.gdrive

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.Test
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.metrics.pid.PIDsGroup
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.PidDefinitionRegistry
import org.obd.metrics.pid.ValueType
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TripUploadTest {

    @Test
    fun `driveTripFileName strips the trip-profile_ prefix and appends json gz`() {
        assertEquals(
            "device123-1-1234567890-120.jsonl.json.gz",
            TripUpload.driveTripFileName("device123", "trip-profile_1-1234567890-120.jsonl")
        )
    }

    @Test
    fun `driveTripFileName leaves the name untouched if the prefix is absent`() {
        assertEquals(
            "device123-some-other-name.json.gz",
            TripUpload.driveTripFileName("device123", "some-other-name")
        )
    }

    @Test
    fun `isReadyForSync takes finished trips that are not uploaded yet`() {
        assertTrue(TripUpload.isReadyForSync("trip-profile_5-1793209935762-743.jsonl"))
        assertFalse(TripUpload.isReadyForSync("trip-profile_5-1793209935762-743.jsonl.synced"))
        assertFalse(TripUpload.isReadyForSync("obd_graphs.backup"))
    }

    @Test
    fun `isReadyForSync skips a trip whose name has no length yet, the one being recorded`() {
        assertFalse(TripUpload.isReadyForSync("trip-profile_5-1793209935762.jsonl"))
    }

    @Test
    fun `buildTransformer leaves metadata such as the VIN out of the upload`() {
        val live = PidDefinition(7005L, 2, "A", "22", "195A", "mbar", "Boost", 0, 3000, ValueType.DOUBLE).apply { group = PIDsGroup.LIVEDATA }
        val vin = PidDefinition(17001L, 17, "", "22", "F190", "", "VIN", 0, 0, ValueType.INT).apply { group = PIDsGroup.METADATA }
        val registry = mockk<PidDefinitionRegistry>()
        // findAll() returns live data only, so metadata must be looked up by its group.
        every { registry.findAll() } returns listOf(live)
        every { registry.findBy(PIDsGroup.METADATA) } returns listOf(vin)
        mockkObject(DataLoggerRepository)
        every { DataLoggerRepository.getPidDefinitionRegistry() } returns registry

        try {
            val result = TripUpload.buildTransformer().transform(
                """
                {"entry":{"x":1.0,"y":"ZARPAHDN1N7D49921","data":17001},"ts":1000,"rawAnswer":""}
                """.trimIndent()
            ).readText()

            assertFalse(result.contains("17001"))
        } finally {
            unmockkAll()
        }
    }
}
