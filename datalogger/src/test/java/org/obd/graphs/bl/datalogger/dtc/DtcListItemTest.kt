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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.obd.graphs.DiagnosticMappingItem
import org.obd.metrics.api.model.DiagnosticTroubleCode
import org.obd.metrics.api.model.SnapshotPID
import org.obd.metrics.command.dtc.DtcComponent
import org.obd.metrics.command.dtc.DtcModuleStatus
import org.obd.metrics.pid.PidDefinition

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
    fun `dtc overlay timeout outlasts a real multi-module scan`() {
        // 13 modules with snapshots + extended data took ~13 s on the 2.0 GME; the old fixed 10 s overlay timed out.
        assertTrue(dtcActionTimeoutMs(13) > 23_000)
        assertTrue(dtcActionTimeoutMs(13) > dtcActionTimeoutMs(1))
        assertEquals(dtcActionTimeoutMs(1), dtcActionTimeoutMs(0))
    }

    @Test
    fun `within a module active codes come first, then stored, pending, history, unknown`() {
        val sorted =
            listOf(
                dtc("P0500", module = "a").apply { statusMask = 0x00 },
                dtc("P0400", module = "a").apply { statusMask = 0x20 },
                dtc("P0300", module = "a").apply { statusMask = 0x04 },
                dtc("P0200", module = "a").apply { statusMask = 0x08 },
                dtc("P0100", module = "b").apply { statusMask = 0x09 },
                dtc("P0600", module = "a", description = null).apply { statusMask = 0x09 }
            ).sortedForDisplay()
        assertEquals(listOf("P0600", "P0200", "P0300", "P0400", "P0500", "P0100"), sorted.map { it.standardCode })
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
                .toDtcListItems(emptyList(), "none", "none for module", unknownModuleLabel = "?")
                .toDtcSections()
        assertEquals(1, sections.size)
        assertEquals(null, sections[0].header)
        assertEquals(2, sections[0].items.size)
    }

    @Test
    fun `no codes is a single message section`() {
        val sections = emptyList<DiagnosticTroubleCode>().toDtcListItems(emptyList(), "none", "x", unknownModuleLabel = "?").toDtcSections()
        assertEquals(listOf(DtcSection(null, listOf(DtcListItem.Message("", "none")))), sections)
    }

    @Test
    fun `scanned modules become sections, empty ones with a message`() {
        val sections =
            listOf(dtc("P0100", module = "Engine"))
                .toDtcListItems(listOf("Engine", "Gearbox"), "none", "nothing", unknownModuleLabel = "?")
                .toDtcSections()
        assertEquals(listOf("Engine", "Gearbox"), sections.map { it.header })
        assertTrue(sections[0].items.single() is DtcListItem.DtcRow)
        assertEquals(DtcListItem.Message("Gearbox", "nothing"), sections[1].items.single())
    }

    @Test
    fun `untagged codes from a multi-module scan go under the unknown module label`() {
        val sections =
            listOf(dtc("P0100", module = "ecu"), dtc("P0105"))
                .toDtcListItems(listOf("Engine ECU", "ABS"), "none", "nothing", unknownModuleLabel = "Unknown module")
                .toDtcSections()
        assertEquals(listOf("Unknown module", "ABS", "Engine ECU"), sections.map { it.header })
        assertEquals(2, sections[0].items.size)
    }

    @Test
    fun `modules reporting codes come before clean ones, each group by name`() {
        val sections =
            listOf(dtc("P0100", module = "Engine ECU"), dtc("C0035", module = "ABS"), dtc("P0105", module = "Engine ECU"))
                .toDtcListItems(listOf("Airbag", "ABS", "Engine ECU", "BCM"), "none", "nothing", unknownModuleLabel = "?")
                .toDtcSections()

        assertEquals(listOf("ABS", "Engine ECU", "Airbag", "BCM"), sections.map { it.header })
        assertEquals(listOf(1, 2, 0, 0), sections.map { it.codeCount })
    }

    @Test
    fun `module header shows the code count only when there are codes`() {
        assertEquals("Engine ECU (2)", DtcListItem.ModuleHeader("Engine ECU", 2).displayName())
        assertEquals("ABS", DtcListItem.ModuleHeader("ABS").displayName())
    }

    @Test
    fun `untagged codes from a single-module scan go under that module`() {
        val sections =
            listOf(dtc("P0100", module = "ecu"))
                .toDtcListItems(listOf("Engine ECU"), "none", "nothing", unknownModuleLabel = "Unknown module")
                .toDtcSections()
        assertEquals(listOf("Engine ECU"), sections.map { it.header })
    }

    @Test
    fun `entries sharing a header are scanned once under the first entry`() {
        val mappings =
            listOf(
                DiagnosticMappingItem(1, "22", "DA10F1", "Engine ECU"),
                DiagnosticMappingItem(2, "GME20", "da10f1", "Engine ECU (2.0 GME)"),
                DiagnosticMappingItem(3, "ABS", "DA28F1", "ABS"),
                DiagnosticMappingItem(4, "BCM", "", "BCM")
            )
        assertEquals(
            listOf("Engine ECU", "ABS"),
            dtcScanTargets(mappings, setOf("22", "GME20", "ABS", "BCM")).map { it.displayName }
        )
        // A deselected first entry leaves the header to the next selected one.
        assertEquals(
            listOf("Engine ECU (2.0 GME)"),
            dtcScanTargets(mappings, setOf("GME20")).map { it.displayName }
        )
    }

    @Test
    fun `the same code from two modules is two rows`() {
        val abs = dtc("U0100", module = "ABS").apply { rawHex = "C10000" }
        val bcm = dtc("U0100", module = "BCM").apply { rawHex = "C10000" }

        val rows =
            listOf(abs, bcm)
                .toDtcListItems(listOf("ABS", "BCM"), "none", "none for module", "unknown")
                .filterIsInstance<DtcListItem.DtcRow>()

        assertEquals(2, rows.size)
        assertNotEquals(rows[0].itemKey(), rows[1].itemKey())
    }

    @Test
    fun `a status change keeps the row identity`() {
        val before = DtcListItem.DtcRow(dtc("P0300", module = "Engine").apply { rawHex = "030008" })
        val after = DtcListItem.DtcRow(dtc("P0300", module = "Engine").apply { rawHex = "030009" })

        assertEquals(before.itemKey(), after.itemKey())
        assertNotEquals(before.itemKey(), DtcListItem.DtcRow(dtc("P0300", module = "Engine", failureType = "1A")).itemKey())
    }

    @Test
    fun `default ECU tags share one identity, unlike the header and message of a module`() {
        assertEquals(
            DtcListItem.DtcRow(dtc("P0300", module = null)).itemKey(),
            DtcListItem.DtcRow(dtc("P0300", module = "ecu")).itemKey()
        )
        assertNotEquals(DtcListItem.ModuleHeader("ABS").itemKey(), DtcListItem.Message("ABS", "none").itemKey())
    }

    @Test
    fun `the read on connect scans every configured module, ignoring the picker`() {
        val mappings =
            listOf(
                DiagnosticMappingItem(1, "22", "DA10F1", "Engine ECU"),
                DiagnosticMappingItem(2, "ABS", "DA28F1", "ABS"),
                DiagnosticMappingItem(3, "BCM", "", "BCM")
            )
        assertEquals(setOf("22", "ABS"), dtcConnectScanModules(mappings))
        assertTrue(dtcConnectScanModules(emptyList()).isEmpty())
    }

    @Test
    fun `did lines flatten tile labels and fall back for missing parts`() {
        fun did(
            description: String?,
            units: String?,
            value: Number?
        ) = SnapshotPID(PidDefinition(1L, 1, "A", "22", "6080", units, description, 0, 255, null), "07", value)

        assertEquals(
            listOf("DTC Event Counter: 7", "Engine Coolant Temp: 90 C", "unknown DID: n/a"),
            listOf(did("DTC Event\nCounter", "", 7), did("Engine Coolant\n Temp", "C", 90), did(null, null, null))
                .toDidLines(unknownDid = "unknown DID", notAvailable = "n/a")
        )
        assertTrue(null.toDidLines("unknown DID", "n/a").isEmpty())
    }

    private fun withStatus(
        code: String,
        module: String,
        statusMask: Int
    ) = dtc(code, module = module).apply { this.statusMask = statusMask }

    @Test
    fun `module headers summarise their codes by state, MIL and new`() {
        val engine =
            listOf(
                withStatus("P0100", "Engine", 0x09), // active, test failed + confirmed
                withStatus("P0200", "Engine", 0x89), // active + MIL
                withStatus("P0300", "Engine", 0x08), // stored
                withStatus("P0400", "Engine", 0x10) // no state
            )
        val items =
            engine
                .toDtcListItems(listOf("Engine", "ABS"), "none", "none for module", "unknown")
                .map { if (it is DtcListItem.DtcRow && it.dtc.standardCode == "P0300") it.copy(isNew = true) else it }
                .withModuleSummaries()

        val headers = items.filterIsInstance<DtcListItem.ModuleHeader>().associateBy { it.module }
        assertEquals(
            DtcModuleSummary(byState = mapOf(DtcState.ACTIVE to 2, DtcState.STORED to 1), milCount = 1, newCount = 1),
            headers.getValue("Engine").summary
        )
        assertEquals(DtcModuleSummary(), headers.getValue("ABS").summary)
    }

    @Test
    fun `modules with codes start expanded, clean ones collapsed, and the user's choice wins`() {
        val items =
            listOf(withStatus("P0100", "Engine", 0x09))
                .toDtcListItems(listOf("Engine", "ABS"), "none", "none for module", "unknown")

        val byDefault = items.collapsedBy { null }
        assertEquals(listOf("header:Engine", "dtc:Engine:P0100", "header:ABS"), byDefault.map { it.itemKey() })
        assertTrue((byDefault[0] as DtcListItem.ModuleHeader).expanded)
        assertFalse((byDefault[2] as DtcListItem.ModuleHeader).expanded)

        val toggled = items.collapsedBy { mapOf("Engine" to false, "ABS" to true)[it] }
        assertEquals(listOf("header:Engine", "header:ABS", "message:ABS"), toggled.map { it.itemKey() })
    }

    @Test
    fun `collapsing a module never hides the no longer reported section`() {
        val gone = dtc("P0500", module = "Engine")
        val diff = DtcHistoryDiff(gone = listOf(gone))
        val items =
            listOf(withStatus("P0100", "Engine", 0x09))
                .toDtcListItems(listOf("Engine"), "none", "none for module", "unknown")
                .collapsedBy { false }
                .withGoneSection(diff, expanded = true)

        assertEquals(listOf("header:Engine", "gone-header", "dtc:Engine:P0500"), items.map { it.itemKey() })
    }

    @Test
    fun `a module that did not answer is not shown as clean and sorts between codes and clean modules`() {
        val items =
            listOf(withStatus("P0100", "Engine", 0x09))
                .toDtcListItems(
                    scannedModules = listOf("Engine", "ABS", "BCM", "Airbag"),
                    noCodesMessage = "none",
                    noCodesForModuleMessage = "no codes",
                    unknownModuleLabel = "unknown",
                    moduleStatus =
                    mapOf(
                        "Engine" to DtcModuleStatus.RESPONDED,
                        "ABS" to DtcModuleStatus.NO_RESPONSE,
                        "BCM" to DtcModuleStatus.RESPONDED,
                        "Airbag" to DtcModuleStatus.REFUSED
                    ),
                    noResponseMessage = "no response",
                    refusedMessage = "refused"
                )

        val headers = items.filterIsInstance<DtcListItem.ModuleHeader>()
        assertEquals(listOf("Engine", "ABS", "Airbag", "BCM"), headers.map { it.module })
        assertEquals(
            listOf(DtcModuleStatus.RESPONDED, DtcModuleStatus.NO_RESPONSE, DtcModuleStatus.REFUSED, DtcModuleStatus.RESPONDED),
            headers.map { it.response }
        )
        val messages = items.filterIsInstance<DtcListItem.Message>().associate { it.key to (it.text to it.status) }
        assertEquals("no response" to DtcModuleStatus.NO_RESPONSE, messages["ABS"])
        assertEquals("refused" to DtcModuleStatus.REFUSED, messages["Airbag"])
        assertEquals("no codes" to DtcModuleStatus.RESPONDED, messages["BCM"])
        // Silent modules have no codes, so they start collapsed like clean ones.
        assertEquals(listOf("header:Engine", "dtc:Engine:P0100", "header:ABS", "header:Airbag", "header:BCM"), items.collapsedBy { null }.map { it.itemKey() })
    }

    @Test
    fun `without recorded statuses every module counts as answered`() {
        val items = emptyList<DiagnosticTroubleCode>().toDtcListItems(listOf("ABS"), "none", "no codes", "unknown")

        assertEquals(DtcModuleStatus.RESPONDED, items.filterIsInstance<DtcListItem.ModuleHeader>().single().response)
        assertEquals("no codes", items.filterIsInstance<DtcListItem.Message>().single().text)
    }

    @Test
    fun `unanswered modules keep the scan order`() {
        val status =
            linkedMapOf(
                "Engine" to DtcModuleStatus.RESPONDED,
                "BCM" to DtcModuleStatus.NO_RESPONSE,
                "ABS" to DtcModuleStatus.REFUSED
            )

        assertEquals(listOf("BCM", "ABS"), status.unansweredModules())
    }

    @Test
    fun `an expanded module with codes ends with its clear action, collapsed and clean modules do not`() {
        val items =
            listOf(dtc("P0100", module = "Engine"), dtc("P0200", module = "Engine"), dtc("C0035", module = "ABS"), dtc("B1000", module = "Unknown"))
                .toDtcListItems(listOf("Engine", "ABS", "BCM"), "none", "none for module", "unknown")
                .collapsedBy { mapOf("ABS" to false, "BCM" to true)[it] }
                .withGoneSection(DtcHistoryDiff(gone = listOf(dtc("P0500", module = "Engine"))), expanded = true)
                .withModuleClearActions { it != "Unknown" }

        assertEquals(
            listOf(
                "header:ABS",
                "header:Engine",
                "dtc:Engine:P0100",
                "dtc:Engine:P0200",
                "clear:Engine",
                "header:Unknown",
                "dtc:Unknown:B1000",
                "header:BCM",
                "message:BCM",
                "gone-header",
                "dtc:Engine:P0500"
            ),
            items.map { it.itemKey() }
        )
    }

    @Test
    fun `a module label maps back to the table entry that scans it`() {
        val mappings =
            listOf(
                DiagnosticMappingItem(1, "22", "DA10F1", "Engine ECU"),
                DiagnosticMappingItem(2, "GME20", "da10f1", "Engine ECU (2.0 GME)"),
                DiagnosticMappingItem(3, "ABS", "DA28F1", "ABS"),
                DiagnosticMappingItem(4, "BCM", "", "BCM")
            )
        assertEquals("22", dtcModuleRequestKey(mappings, "Engine ECU"))
        assertEquals("ABS", dtcModuleRequestKey(mappings, "ABS"))
        // Never scanned under that name: a header shared with an earlier entry, no header, not in the table.
        assertEquals(null, dtcModuleRequestKey(mappings, "Engine ECU (2.0 GME)"))
        assertEquals(null, dtcModuleRequestKey(mappings, "BCM"))
        assertEquals(null, dtcModuleRequestKey(mappings, "ECU"))
    }
}
