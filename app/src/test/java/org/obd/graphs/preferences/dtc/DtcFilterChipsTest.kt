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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.dtc.DtcFilter
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class DtcFilterChipsTest {
    // A state chip and the filter for its state read the same, and share its icon and colour.
    @Test
    fun `every state filter is named and drawn like its state chip`() {
        DtcFilter.entries.filter { it.state != null }.forEach { filter ->
            val style = STATE_BADGES.getValue(filter.state!!)
            assertEquals(filter.name, DTC_LEGEND.single { it.second == style }.first, filter.labelRes())
        }
        assertEquals(R.string.dtc_filter_all, DtcFilter.ALL.labelRes())
    }

    // The chip text is formatted with a label and a count: a resource without both would throw on every refresh.
    @Test
    fun `filter strings exist in every language, the chip takes a label and a count`() {
        listOf("values", "values-pl").forEach { dir ->
            val nodes =
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(File("src/main/res/$dir/strings.xml"))
                    .getElementsByTagName("string")
            val strings = (0 until nodes.length).map { nodes.item(it) }.associate { it.attributes.getNamedItem("name").nodeValue to it.textContent }
            listOf("dtc.filter.all", "dtc.filter.empty").forEach { assertTrue("$dir: $it", strings[it].orEmpty().isNotBlank()) }
            val chip = strings.getValue("dtc.filter.chip")
            assertTrue("$dir: $chip", chip.contains("%1\$s") && chip.contains("%2\$d"))
        }
    }
}
