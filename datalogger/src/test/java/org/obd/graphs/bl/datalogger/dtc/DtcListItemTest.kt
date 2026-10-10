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
import org.obd.graphs.DiagnosticMappingItem
import org.obd.metrics.api.model.DiagnosticTroubleCode
import org.obd.metrics.command.dtc.DtcComponent

class DtcListItemTest {
    private fun dtc(
        code: String,
        description: String? = "desc $code",
        module: String? = null,
        failureType: String? = null
    ) = DiagnosticTroubleCode().apply {
        standardCode = code
        this.description = description
        this.module = module
        failureType?.let { this.failureType = DtcComponent(it, "") }
    }

    @Test
    fun `display code appends the failure type only when present`() {
        assertEquals("P0123-1A", dtc("P0123", failureType = "1A").displayCode())
        assertEquals("P0123", dtc("P0123", failureType = "").displayCode())
        assertEquals("P0123", dtc("P0123").displayCode())
    }

    @Test
    fun `unknown description falls back to the component path`() {
        val code =
            dtc("P0123", description = "Unknown DTC Description").apply {
                system = DtcComponent("P", "Powertrain")
                category = DtcComponent("0", "Generic")
            }
        assertTrue(code.isDescriptionUnknown())
        assertEquals("Powertrain → Generic (Unknown specific fault)", code.displayDescription())
        assertEquals("Unknown DTC Description", dtc("P0123", description = null).displayDescription())
        assertEquals("desc P0123", dtc("P0123").displayDescription())
    }

    @Test
    fun `sorted by module, known descriptions first, then code`() {
        val sorted =
            listOf(
                dtc("P0300", module = "b"),
                dtc("P0200", module = "a", description = null),
                dtc("P0100", module = "a", description = null),
                dtc("P0400", module = "a")
            ).sortedForDisplay()
        assertEquals(listOf("P0400", "P0100", "P0200", "P0300"), sorted.map { it.standardCode })
    }

    @Test
    fun `scan modules skip deselected and header-less mappings`() {
        val mappings =
            listOf(
                DiagnosticMappingItem(1, "ecm", "7E0"),
                DiagnosticMappingItem(2, "tcm", "7E1"),
                DiagnosticMappingItem(3, "abs", "")
            )
        assertEquals(setOf("ecm"), dtcScanModules(mappings, setOf("tcm")))
        assertEquals(setOf("ecm", "tcm"), dtcScanModules(mappings, emptySet()))
        assertEquals(emptySet<String>(), dtcScanModules(emptyList(), emptySet()))
    }

    @Test
    fun `default ECU read is one headerless section`() {
        val sections =
            listOf(dtc("P0100"), dtc("P0200", module = "ecu"))
                .toDtcListItems(emptyList(), "none", "none for module")
                .toDtcSections()
        assertEquals(1, sections.size)
        assertEquals(null, sections[0].header)
        assertEquals(2, sections[0].items.size)
    }

    @Test
    fun `no codes is a single message section`() {
        val sections = emptyList<DiagnosticTroubleCode>().toDtcListItems(emptyList(), "none", "x").toDtcSections()
        assertEquals(listOf(DtcSection(null, listOf(DtcListItem.Message("", "none")))), sections)
    }

    @Test
    fun `scanned modules become sections, empty ones with a message`() {
        val sections =
            listOf(dtc("P0100", module = "Engine"))
                .toDtcListItems(listOf("Engine", "Gearbox"), "none", "nothing")
                .toDtcSections()
        assertEquals(listOf("Engine", "Gearbox"), sections.map { it.header })
        assertTrue(sections[0].items.single() is DtcListItem.DtcRow)
        assertEquals(DtcListItem.Message("Gearbox", "nothing"), sections[1].items.single())
    }
}
