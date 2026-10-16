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

// The workflow explanation names the labels and the button the dialog shows: a renamed label must not leave the
// explanation talking about one that no longer exists, in either language.
class DtcHistoryHelpTest {
    private fun strings(folder: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/$folder/strings.xml"))
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length).associate { i ->
            val node = nodes.item(i)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }

    @Test
    fun `the explanation names the labels and the clear button as the dialog shows them`() {
        listOf("values", "values-pl").forEach { folder ->
            val strings = strings(folder)
            val help = strings.getValue("dtc.history.help.body")
            listOf("dtc.history.new", "dtc.history.gone_title", "dtc.action_clear_codes").forEach { key ->
                assertTrue("$folder: help does not mention $key \"${strings[key]}\"", help.contains(strings.getValue(key)))
            }
            listOf("dtc.history.help.chip", "dtc.history.help.link", "dtc.history.help.title").forEach { key ->
                assertTrue("$folder: $key missing", strings[key]?.isNotBlank() == true)
            }
        }
    }

    // The text scrolls inside a capped height, so the Close button always fits under it.
    @Test
    fun `the help text leaves room for the close button`() {
        assertEquals(1152, helpMessageMaxHeight(1920))
        assertTrue(helpMessageMaxHeight(720) < 720 - 200)
    }
}
