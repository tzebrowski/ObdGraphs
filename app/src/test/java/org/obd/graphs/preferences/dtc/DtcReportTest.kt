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
import org.junit.Test
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class DtcReportTest {
    private val labels =
        DtcReportHeaderLabels(
            duration = "Scan duration: %1\$.1f s",
            noResponse = "Modules without an answer: %1\$s"
        )

    @Test
    fun `the header tells when and from which vehicle, how long the scan took and who did not answer`() {
        assertEquals(
            listOf(
                "Last read: 19 Oct 2026, 10:42 · VIN ZARFAMAN0K7612345",
                "Scan duration: 13.4 s",
                "Modules without an answer: ABS, TCM"
            ),
            dtcReportHeader(
                "Last read: 19 Oct 2026, 10:42 · VIN ZARFAMAN0K7612345",
                13_420,
                listOf("ABS", "TCM"),
                labels,
                Locale.US
            )
        )
    }

    // Lists stored before the duration was recorded, and reads before any read info: no "N/A" lines.
    @Test
    fun `unknown parts are left out`() {
        assertEquals(emptyList<String>(), dtcReportHeader(null, null, emptyList(), labels, Locale.US))
        assertEquals(listOf("Last read: 10:42"), dtcReportHeader("Last read: 10:42", null, emptyList(), labels, Locale.US))
    }

    @Test
    fun `the duration follows the locale's decimal separator`() {
        assertEquals(listOf("Scan duration: 13,4 s"), dtcReportHeader(null, 13_420, emptyList(), labels, Locale("pl", "PL")))
    }

    // The format strings are used with a number: a resource without %1$.1f would throw at share time.
    @Test
    fun `the duration string takes a decimal in every language`() {
        listOf("values", "values-pl").forEach { dir ->
            val strings = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/$dir/strings.xml"))
            val nodes = strings.getElementsByTagName("string")
            val duration = (0 until nodes.length).map { nodes.item(it) }.single { it.attributes.getNamedItem("name").nodeValue == "dtc.share.duration" }
            assertEquals("$dir: ${duration.textContent}", true, duration.textContent.contains("%1\$.1f"))
        }
    }
}
