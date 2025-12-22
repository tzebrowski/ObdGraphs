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

import org.assertj.core.api.Assertions
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

fun main() {
    try {
        val rawJson = File("integrations/src/test/assets/", "trip-profile_1-1765481895809-22.json").readText()
        val transformer: VehicleLogTransformer = DefaultTransformer()
        val json = transformer.transform(rawJson)
        println("Optimization successful!")
        println(json)
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}


class VehicleLogTransformerTest {

    private val transformer: VehicleLogTransformer = DefaultTransformer()

    @Test
    fun `optimize should convert complex json to optimized flat format`() {
        val rawJson = """
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

        val result = transformer.transform(rawJson)

        val expectedJson =
            """{"startTs":123456789,"entries":{"10":{"metrics":[{"v":50.5,"t":1000},{"v":60.5,"t":2000}],"min":1.5,"max":3.0,"mean":2.25}}}"""

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

        val result = transformer.transform(rawJson)
        assertFalse (result.contains("\"id\"") )
        assertFalse(result.contains("\"x\""))
        assertFalse(result.contains("\"data\""))
        assertFalse(result.contains("\"rawAnswer\""))
        assertFalse(result.contains("\"entry\""))

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

        val result = transformer.transform(rawJson)

        val expected = """{"startTs":100,"entries":{}}"""
        assertEquals(expected, result)
    }
}
