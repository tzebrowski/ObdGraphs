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
package org.obd.graphs.bl.trip

import io.mockk.every
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.TestSetup
import org.obd.metrics.transport.message.ConnectorResponseFactory
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val PREF_SAVE_CONNECTOR_RESPONSE = "pref.debug.trip.save.connector_response"

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TripModelSerializerTest : TestSetup() {

    @Before
    override fun setup() {
        super.setup()
    }

    @After
    fun tearDown() = unmockkAll()

    private fun metricWithResponse(message: String) =
        Metric(
            entry = Entry(x = 1.5f, y = 42f, data = 7L),
            ts = 123456789L,
            rawAnswer = ConnectorResponseFactory.wrap(message.toByteArray())
        )

    @Test
    fun `serializer includes the raw connector response when the debug preference is enabled`() {
        every { sharedPrefs.getBoolean(eq(PREF_SAVE_CONNECTOR_RESPONSE), any()) } returns true

        val json = TripModelSerializer().serializer.writeValueAsString(metricWithResponse("some-response"))

        assertTrue(json.contains("some-response"))
    }

    @Test
    fun `serializer omits the raw connector response when the debug preference is disabled`() {
        every { sharedPrefs.getBoolean(eq(PREF_SAVE_CONNECTOR_RESPONSE), any()) } returns false

        val json = TripModelSerializer().serializer.writeValueAsString(metricWithResponse("some-response"))

        assertFalse(json.contains("some-response"))
    }

    @Test
    fun `deserializer round-trips entry and timestamp but always yields an empty connector response`() {
        every { sharedPrefs.getBoolean(eq(PREF_SAVE_CONNECTOR_RESPONSE), any()) } returns true

        val serializer = TripModelSerializer()
        val original = metricWithResponse("some-response")

        val json = serializer.serializer.writeValueAsString(original)
        val roundTripped = serializer.deserializer.readValue(json, Metric::class.java)

        // Compare fields individually rather than via Entry.equals(): Jackson deserializes
        // the "y: Any" property as a Double regardless of the original numeric type, so a
        // whole-object comparison against the original Float would spuriously fail.
        assertEquals(original.entry.x, roundTripped.entry.x)
        assertEquals(original.entry.data, roundTripped.entry.data)
        assertEquals(original.entry.y.toString().toFloat(), roundTripped.entry.y.toString().toFloat())
        assertEquals(original.ts, roundTripped.ts)
        // ConnectorResponseDeserializer always returns the fixed empty response,
        // regardless of what was serialized - this is the real, current behavior.
        assertEquals("", roundTripped.rawAnswer.message)
    }
}
