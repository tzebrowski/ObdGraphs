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

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Properties

private const val DESELECTED = "pref.dtc.module_picker.deselected"
private const val LEGACY = "Legacy Mode 01"

// The bundled profiles start with the "OBD-II (Legacy Mode 01)" entry unticked in the DTC module picker: scanned with
// UDS, the 2.0 GME's DB33F1 only repeats the Engine ECU's codes. A profile adding the entry must preset it too.
class DtcProfileDefaultsTest {
    private val profiles: List<Properties> =
        listOf("src/main/assets", "../ext/src/main/assets", "../automotive/src/main/assets")
            .flatMap { File(it).listFiles { f -> f.name.endsWith(".properties") }.orEmpty().toList() }
            .map { file -> Properties().apply { file.inputStream().use { load(it) } } }

    private fun Properties.legacyEntries(): List<Pair<String, String>> =
        stringPropertyNames()
            .filter { it.contains(".pref.adapter.init.mode.description_value.") && getProperty(it).contains(LEGACY) }
            .map { key ->
                val profile = key.substringBefore(".")
                val requestKey = getProperty(key.replace("description_value", "id_value")).replace("\"", "")
                profile to requestKey
            }

    @Test
    fun `every legacy entry starts deselected in the module picker`() {
        val entries = profiles.flatMap { props -> props.legacyEntries().map { props to it } }
        assertTrue("no profile with a legacy entry found", entries.isNotEmpty())

        entries.forEach { (props, entry) ->
            val (profile, requestKey) = entry
            val deselected =
                props
                    .getProperty("$profile.$DESELECTED", "")
                    .removePrefix("[")
                    .removeSuffix("]")
                    .split(",")
                    .map { it.trim() }
            assertTrue("$profile: $requestKey not in $DESELECTED", requestKey in deselected)
        }
    }
}
