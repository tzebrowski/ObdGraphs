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
package org.obd.graphs.preferences.dtc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URLDecoder
import java.util.Properties

class DtcSearchTest {
    private val labels =
        DtcPromptLabels(
            code = "Diagnostic trouble code %1\$s",
            codeWithDescription = "Diagnostic trouble code %1\$s (%2\$s)",
            vehicle = "Vehicle: %1\$s",
            module = "Reported by: %1\$s",
            status = "Status: %1\$s",
            question = "Explain it"
        )

    @Test
    fun `prompt names the vehicle, module and status`() {
        val prompt =
            dtcAiPrompt(
                labels,
                code = "P0120-14",
                description = "Throttle position sensor circuit.",
                failureType = "Failure type: 14 · Short to ground",
                vehicle = vehicleDescription("Alfa Romeo", "Giulia", "2.0 GME"),
                module = "ECM",
                status = "Active"
            )

        assertEquals(
            "Diagnostic trouble code P0120-14 (Throttle position sensor circuit). Failure type: 14 · Short to ground. " +
                "Vehicle: Alfa Romeo Giulia 2.0 GME. Reported by: ECM. Status: Active. Explain it",
            prompt
        )
    }

    @Test
    fun `unknown parts are left out`() {
        val prompt = dtcAiPrompt(labels, "U0100", " ", null, vehicleDescription(" ", null, ""), null, null)

        assertEquals("Diagnostic trouble code U0100. Explain it", prompt)
    }

    @Test
    fun `vehicle is built from the filled settings only`() {
        assertEquals("Alfa Romeo 2.0 GME", vehicleDescription("Alfa Romeo", "", " 2.0 GME "))
        assertNull(vehicleDescription(null, " ", ""))
    }

    @Test
    fun `url opens Google AI Mode with the whole prompt`() {
        val prompt = "Diagnostic trouble code P0120 (Throttle & pedal). Vehicle: Alfa Romeo"
        val url = dtcAiSearchUrl(prompt)

        assertTrue(url, url.startsWith("https://www.google.com/search?udm=50&q="))
        assertEquals(prompt, URLDecoder.decode(url.substringAfter("&q="), "UTF-8"))
    }

    @Test
    fun `every bundled vehicle profile presets make and engine`() {
        val profiles =
            listOf("src/main/assets", "../ext/src/main/assets", "../automotive/src/main/assets")
                .flatMap { File(it).listFiles { f -> f.name.endsWith(".properties") }.orEmpty().toList() }
                .filterNot { it.name.startsWith("default") }
        assertFalse("no profiles found", profiles.isEmpty())

        profiles.forEach { file ->
            val props = Properties().apply { file.inputStream().use { load(it) } }
            val id = props.getProperty(props.stringPropertyNames().first { it.endsWith(".pref.profile.id") })
            listOf(PREF_VEHICLE_MAKE, PREF_VEHICLE_ENGINE).forEach { key ->
                assertFalse("${file.name}: $id.$key", props.getProperty("$id.$key").isNullOrBlank())
            }
        }
    }
}
