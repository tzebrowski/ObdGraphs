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
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class DtcListSummaryTest {
    private val labels =
        DtcListSummaryLabels(
            notReadYet = "Not read yet",
            noCodes = "No codes",
            codes = { "$it codes" }
        )

    @Test
    fun `the row tells the code count and the last read`() {
        assertEquals("3 codes · Last read: 10:42", dtcListSummary(3, "Last read: 10:42", labels))
        assertEquals("No codes · Last read: 10:42", dtcListSummary(0, "Last read: 10:42", labels))
    }

    // A count without a read cannot be trusted (codes stored before read info existed): ask for a read instead.
    @Test
    fun `before the first read the row asks for one`() {
        assertEquals("Not read yet", dtcListSummary(0, null, labels))
        assertEquals("Not read yet", dtcListSummary(2, null, labels))
    }

    private fun resources(folder: String): org.w3c.dom.Document =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/$folder/strings.xml"))

    private fun strings(folder: String): Map<String, String> {
        val nodes = resources(folder).getElementsByTagName("string")
        return (0 until nodes.length).associate { i ->
            val node = nodes.item(i)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }

    private fun pluralQuantities(
        folder: String,
        name: String
    ): Set<String> {
        val plurals = resources(folder).getElementsByTagName("plurals")
        val node = (0 until plurals.length).map { plurals.item(it) }.single { it.attributes.getNamedItem("name").nodeValue == name }
        val items = (node as org.w3c.dom.Element).getElementsByTagName("item")
        return (0 until items.length).map { items.item(it).attributes.getNamedItem("quantity").nodeValue }.toSet()
    }

    // Polish needs few / many: "2 kody", "5 kodów"; a missing quantity falls back to "other" ("5 kodu").
    @Test
    fun `the code count has every plural form the language needs`() {
        assertTrue(pluralQuantities("values", "pref.dtc.list_codes").containsAll(setOf("one", "other")))
        assertTrue(pluralQuantities("values-pl", "pref.dtc.list_codes").containsAll(setOf("one", "few", "many", "other")))
    }

    // The automatic clear points to the manual one by its button label: a renamed button must not leave it stale.
    @Test
    fun `the automatic clear warning names the clear button as the dialog shows it`() {
        listOf("values", "values-pl").forEach { folder ->
            val strings = strings(folder)
            val warning = strings.getValue("pref.dtc.enable_automatic_cleanup_summary")
            assertTrue("$folder: $warning", warning.contains(strings.getValue("dtc.action_clear_codes")))
        }
    }
}
