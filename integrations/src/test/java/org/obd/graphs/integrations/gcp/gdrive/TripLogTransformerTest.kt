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
package org.obd.graphs.integrations.gcp.gdrive

import android.content.SharedPreferences
import android.util.Log
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.obd.graphs.integrations.log.TripLog
import org.obd.graphs.integrations.log.TripLogTransformer
import org.obd.graphs.preferences.Prefs
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TripLogTransformerTest {
    protected val sharedPrefs = mockk<SharedPreferences>(relaxed = true)
    protected val editor = mockk<SharedPreferences.Editor>(relaxed = true)

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
        every { sharedPrefs.getBoolean(any(), any()) } returns false
        every { sharedPrefs.registerOnSharedPreferenceChangeListener(any()) } just Runs
    }

    @Before
    fun setup() {
        mockPrefs()
        mockLog()
    }

    @Test
    fun `read file test`() {

        val file = File("src/test/assets/", "trip-profile_1-1765481895809-22.json")
        val transformer: TripLogTransformer = TripLog.transformer { s, v -> v }
        val result = transformer.transform(file).readText()

        Assertions.assertThat(result).startsWith("[{\"t\":1765481896083,\"s\":\"12\",\"v\":3298.0767},{\"t\":1765481896267,\"s\":\"12\",\"v\":3298.0767},{\"t\":1765481896463,\"s\":\"12\",\"v\":3298.0767},{\"t\":1765481896666,\"s\":\"12\"")
    }

    @Test
    fun `meta test`() {
        val rawJson =
            """
            {
              "startTs": 123456789,
              "entries": {
                "10": {
                  "id": 999,
                  "min": 1.5,
                  "max": 3.0,
                  "mean": 2.25,
                  "metrics": [
                    {
                      "entry": { "x": 100.0, "y": 50.5, "data": 12 },
                      "ts": 1000,
                      "rawAnswer": "ignore me"
                    },
                    {
                      "entry": { "x": 101.0, "y": 60.5, "data": 13 },
                      "ts": 2000,
                      "rawAnswer": ""
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        val signalMapper = mapOf(12 to "Boost", 14 to "Engine speed")
        val transformer: TripLogTransformer = TripLog.transformer(signalMapper=signalMapper) { s, v ->
            if (v is Number) v.toFloat() * 2 else v.toString()
        }
        val meta = mutableMapOf<String, String>()
        meta["key1"] = "value1"
        meta["key2"] = "value2"

        val result = transformer.transform(rawJson, meta).readText()

        val expectedJson =
            """[{"metadata":{"key1":"value1","key2":"value2"}},{"t":1000,"s":"Boost","v":101.0},{"t":2000,"s":"13","v":121.0}]"""

        Assertions.assertThat(expectedJson).isEqualTo(result)
    }

    @Test
    fun `signal transformation test`() {
        val rawJson =
            """
            {
              "startTs": 123456789,
              "entries": {
                "10": {
                  "id": 999,
                  "min": 1.5,
                  "max": 3.0,
                  "mean": 2.25,
                  "metrics": [
                    {
                      "entry": { "x": 100.0, "y": 50.5, "data": 12 },
                      "ts": 1000,
                      "rawAnswer": "ignore me"
                    },
                    {
                      "entry": { "x": 101.0, "y": 60.5, "data": 13 },
                      "ts": 2000,
                      "rawAnswer": ""
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        val signalMapper = mapOf(12 to "Boost", 14 to "Engine speed")
        val transformer: TripLogTransformer = TripLog.transformer(signalMapper=signalMapper) { s, v -> if (v is Number) v.toFloat() * 2 else v.toString() }
        val result = transformer.transform(rawJson).readText()

        val expectedJson =
            """[{"t":1000,"s":"Boost","v":101.0},{"t":2000,"s":"13","v":121.0}]"""

        Assertions.assertThat(expectedJson).isEqualTo(result)
    }

    @Test
    fun `optimize should convert complex json to optimized flat format`() {
        val rawJson =
            """
            {
              "startTs": 123456789,
              "entries": {
                "10": {
                  "id": 999,
                  "min": 1.5,
                  "max": 3.0,
                  "mean": 2.25,
                  "metrics": [
                    {
                      "entry": { "x": 100.0, "y": 50.5, "data": 12 },
                      "ts": 1000,
                      "rawAnswer": "ignore me"
                    },
                    {
                      "entry": { "x": 101.0, "y": 60.5, "data": 12 },
                      "ts": 2000,
                      "rawAnswer": ""
                    }
                  ]
                }
              }
            }
            """.trimIndent()

        val transformer: TripLogTransformer = TripLog.transformer { s, v -> v }
        val result = transformer.transform(rawJson).readText()

        val expectedJson =
            """[{"t":1000,"s":"12","v":50.5},{"t":2000,"s":"12","v":60.5}]"""

        Assertions.assertThat(expectedJson).isEqualTo(result)
    }

    @Test
    fun `optimize should remove unwanted fields`() {
        // GIVEN
        val rawJson = """
            {
              "startTs": 0,
              "entries": {
                "1": {
                  "id": 123,
                  "min": 0.0, "max": 0.0, "mean": 0.0,
                  "metrics": [ { "entry": { "x": 1.0, "y": 2.0, "data": 5 }, "ts": 100, "rawAnswer": "test" } ]
                }
              }
            }
        """

        val transformer: TripLogTransformer = TripLog.transformer() { s, v -> v }
        val result = transformer.transform(rawJson).bufferedReader().use { it.readText() }

        assertFalse(result.contains("\"id\""))
        assertFalse(result.contains("\"x\""))
        assertFalse(result.contains("\"rawAnswer\""))
        assertFalse(result.contains("\"entry\""))

        assertTrue(result.contains("\"s\""))
        assertTrue(result.contains("\"t\""))
        assertTrue(result.contains("\"v\":2.0"))
    }

    @Test
    fun `optimize should handle empty entries map`() {
        // GIVEN
        val rawJson = """
            {
              "startTs": 100,
              "entries": {}
            }
        """

        val transformer: TripLogTransformer = TripLog.transformer { s, v -> v }
        val result = transformer.transform(rawJson).readText()

        val expected = """[]"""
        assertEquals(expected, result)
    }
}
