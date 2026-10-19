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
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

private const val ANDROID = "http://schemas.android.com/apk/res/android"

// The DTC dialog's buttons: one row, Refresh the only filled one, Clear Codes outlined (destructive, not primary).
class DtcDialogButtonsTest {
    private val container: Element =
        DocumentBuilderFactory
            .newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(File("src/main/res/layout/dialog_dtc.xml"))
            .getElementsByTagName("LinearLayout")
            .let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } }
            .single { it.getAttributeNS(ANDROID, "id") == "@+id/button_container" }

    private val buttons: Map<String, Element> =
        (0 until container.childNodes.length)
            .map { container.childNodes.item(it) }
            .filterIsInstance<Element>()
            .filter { it.tagName.endsWith("Button") }
            .associateBy { it.getAttributeNS(ANDROID, "id").removePrefix("@+id/") }

    @Test
    fun `all four actions sit in one row`() {
        assertEquals("horizontal", container.getAttributeNS(ANDROID, "orientation"))
        assertEquals(listOf("action_share", "action_close_window", "action_clear_dtc", "action_refresh_dtc"), buttons.keys.toList())
    }

    @Test
    fun `refresh is the only filled button, clear codes is outlined`() {
        assertTrue(buttons.getValue("action_refresh_dtc").getAttribute("style").endsWith("UnelevatedButton"))
        assertTrue(buttons.getValue("action_clear_dtc").getAttribute("style").endsWith("OutlinedButton"))
        listOf("action_share", "action_close_window").forEach {
            assertTrue(it, buttons.getValue(it).getAttribute("style").endsWith("TextButton"))
        }
    }

    // Share is an icon only: without a description a screen reader announces an unlabelled button.
    @Test
    fun `the icon-only share button has a description`() {
        assertEquals("@string/dtc.action_share", buttons.getValue("action_share").getAttributeNS(ANDROID, "contentDescription"))
    }
}
