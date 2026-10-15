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
package org.obd.graphs.bl.datalogger.dtc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.obd.graphs.commons.R
import org.obd.metrics.api.model.DiagnosticTroubleCode
import org.obd.metrics.command.dtc.DtcComponent
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class DtcFailureTypeTest {
    // Resolves a string id to its resource name, so the tests read which entry was picked.
    private val names: Map<Int, String> = R.string::class.java.fields.associate { it.getInt(null) to it.name }
    private val getString: (Int) -> String = { id -> if (id == R.string.dtc_ftb_line) "%1\$s=%2\$s" else names.getValue(id) }

    private fun dtc(
        failureType: String?,
        description: String = "Unknown Subtype"
    ) = DiagnosticTroubleCode().apply {
        standardCode = "P0120"
        failureType?.let { this.failureType = DtcComponent(it, description) }
    }

    private fun strings(folder: String): Map<String, String> {
        val document =
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("../common/src/main/res/$folder/strings.xml"))
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length).associate { i ->
            val node = nodes.item(i)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }

    @Test
    fun `a known byte gets its specific text`() {
        assertEquals("dtc_ftb_14", failureTypeText("14", null, getString))
        assertEquals("dtc_ftb_1A", failureTypeText("1a", null, getString))
        assertEquals("dtc_ftb_98", failureTypeText(" 98 ", "Unknown Subtype", getString))
    }

    @Test
    fun `an unlisted byte falls back to the decoder text, then to its category`() {
        assertEquals("Supplier text", failureTypeText("F3", "Supplier text", getString))
        assertEquals("dtc_ftb_category_F", failureTypeText("F3", "Unknown Subtype", getString))
        assertEquals("dtc_ftb_category_2", failureTypeText("2E", "", getString))
        assertEquals("dtc_ftb_category_0", failureTypeText("00", null, getString))
        // The table wins over the decoder: its text is translated, ObdMetrics' is not.
        assertEquals("dtc_ftb_13", failureTypeText("13", "Circuit open", getString))
    }

    @Test
    fun `anything that is not one hex byte or a reserved one has no text`() {
        assertNull(failureTypeText(null, "x", getString))
        assertNull(failureTypeText("", "x", getString))
        assertNull(failureTypeText("ZZ", "x", getString))
        assertNull(failureTypeText("123", "x", getString))
        assertNull(failureTypeText("A5", "Unknown Subtype", getString))
    }

    @Test
    fun `the line names the byte and its meaning`() {
        assertEquals("14=dtc_ftb_14", dtc("14").failureTypeLine(getString))
        assertEquals("1A=dtc_ftb_1A", dtc("1a").failureTypeLine(getString))
        assertNull(dtc(null).failureTypeLine(getString))
        assertNull(dtc("").failureTypeLine(getString))
    }

    @Test
    fun `every entry points at its own string, present in English and Polish`() {
        val entries =
            FAILURE_TYPES.map { (byte, id) -> "dtc_ftb_%02X".format(byte) to id } +
                FAILURE_TYPE_CATEGORIES.map { (nibble, id) -> "dtc_ftb_category_%X".format(nibble) to id }
        val english = strings("values")
        val polish = strings("values-pl")
        entries.forEach { (name, id) ->
            assertEquals(name, names[id])
            assertTrue("$name missing in EN", english[name]?.isNotBlank() == true)
            assertTrue("$name missing in PL", polish[name]?.isNotBlank() == true)
        }
        listOf("values", "values-pl").forEach { folder ->
            assertTrue("$folder: dtc_ftb_line", strings(folder)["dtc_ftb_line"]?.contains("%2\$s") == true)
        }
    }
}
