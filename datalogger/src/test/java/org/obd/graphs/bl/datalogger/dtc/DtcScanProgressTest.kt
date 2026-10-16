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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.obd.graphs.commons.R
import org.obd.metrics.command.dtc.DtcProgress
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class DtcScanProgressTest {
    private val formats =
        mapOf(
            R.string.dtc_progress_module to "%1\$s · %2\$d / %3\$d",
            R.string.dtc_progress_follow_ups to "details · %1\$d"
        )
    private val getString: (Int) -> String = { formats.getValue(it) }

    @Test
    fun `a module step names the module and counts it`() {
        val progress = DtcProgress.module("Engine ECU", 4, 13).toScanProgress()
        assertEquals(DtcScanProgress("Engine ECU", 4, 13), progress)
        assertEquals("Engine ECU · 4 / 13", progress.text(getString))
    }

    @Test
    fun `the per-code step counts the codes`() {
        val progress = DtcProgress.followUps(12).toScanProgress()
        assertEquals(DtcScanProgress(null, 0, 12), progress)
        assertEquals("details · 12", progress.text(getString))
    }

    @Test
    fun `both texts exist in English and Polish with their arguments`() {
        listOf("values", "values-pl").forEach { folder ->
            val nodes =
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(File("../common/src/main/res/$folder/strings.xml"))
                    .getElementsByTagName("string")
            val strings =
                (0 until nodes.length).associate { i ->
                    nodes.item(i).attributes.getNamedItem("name").nodeValue to nodes.item(i).textContent
                }
            assertTrue("$folder: module", strings["dtc_progress_module"]?.contains("%3\$d") == true)
            assertTrue("$folder: follow-ups", strings["dtc_progress_follow_ups"]?.contains("%1\$d") == true)
        }
    }
}
