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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.obd.metrics.api.model.DiagnosticTroubleCode

class DtcStatusTest {
    private val labels = DtcStatusLabels("Active", "Stored", "Pending", "History", "MIL on", "Permanent")

    private fun obdCode(
        statusMask: Int,
        permanent: Boolean
    ) = DiagnosticTroubleCode().apply {
        standardCode = "P0420"
        this.statusMask = statusMask
        isPermanent = permanent
    }

    @Test
    fun `test failed now is active, whatever else is set`() {
        assertEquals(DtcState.ACTIVE, dtcStatus(0x01).state)
        assertEquals(DtcState.ACTIVE, dtcStatus(0x2F).state)
        assertEquals(DtcState.ACTIVE, dtcStatus(0xFF).state)
    }

    @Test
    fun `confirmed without a current failure is stored`() {
        assertEquals(DtcState.STORED, dtcStatus(0x08).state)
        assertEquals(DtcState.STORED, dtcStatus(0x2C).state)
    }

    @Test
    fun `pending without confirmation is pending`() {
        assertEquals(DtcState.PENDING, dtcStatus(0x04).state)
        assertEquals(DtcState.PENDING, dtcStatus(0x26).state)
    }

    @Test
    fun `failed since last clear only is history`() {
        assertEquals(DtcState.HISTORY, dtcStatus(0x20).state)
        assertEquals(DtcState.HISTORY, dtcStatus(0x02).state)
    }

    @Test
    fun `no fault bit is unknown`() {
        assertEquals(DtcState.UNKNOWN, dtcStatus(0x00).state)
        assertEquals(DtcState.UNKNOWN, dtcStatus(0x50).state)
    }

    @Test
    fun `warning lamp is independent of the state`() {
        assertTrue(dtcStatus(0x88).warningLamp)
        assertTrue(dtcStatus(0x80).warningLamp)
        assertFalse(dtcStatus(0x7F).warningLamp)
    }

    @Test
    fun `status line joins state and lamp`() {
        assertEquals("Active · MIL on", dtcStatus(0x89).statusLine(labels))
        assertEquals("Stored", dtcStatus(0x08).statusLine(labels))
        assertEquals("History", dtcStatus(0x20).statusLine(labels))
        assertEquals("MIL on", dtcStatus(0x80).statusLine(labels))
        assertNull(dtcStatus(0x00).statusLine(labels))
    }

    // OBD-II service 0A: permanent is a flag beside the state, as stored (03) and pending (07) can report the same code.
    @Test
    fun `permanent is independent of the state`() {
        assertEquals(DtcStatus(DtcState.UNKNOWN, warningLamp = false, permanent = true), obdCode(0x00, true).dtcStatus())
        assertEquals(DtcStatus(DtcState.STORED, warningLamp = false, permanent = true), obdCode(0x08, true).dtcStatus())
        assertFalse(obdCode(0x08, false).dtcStatus().permanent)
        assertEquals("Permanent", obdCode(0x00, true).dtcStatus().statusLine(labels))
        assertEquals("Stored · Permanent", obdCode(0x08, true).dtcStatus().statusLine(labels))
        assertNull(obdCode(0x00, true).dtcStatus().stateLabel(labels))
    }

    @Test
    fun `only a code with no state besides permanent is permanent only`() {
        assertTrue(obdCode(0x00, true).isPermanentOnly())
        assertFalse(obdCode(0x08, true).isPermanentOnly())
        assertFalse(obdCode(0x04, true).isPermanentOnly())
        assertFalse(obdCode(0x00, false).isPermanentOnly())
    }

    @Test
    fun `state label leaves the lamp out`() {
        assertEquals("Active", dtcStatus(0x89).stateLabel(labels))
        assertEquals("Pending", dtcStatus(0x84).stateLabel(labels))
        assertNull(dtcStatus(0x80).stateLabel(labels))
    }

    @Test
    fun `AA row icon follows the state, permanent only without one`() {
        assertEquals(DtcRowIcon.ACTIVE, dtcStatus(0x09).rowIcon())
        assertEquals(DtcRowIcon.STORED, dtcStatus(0x08).rowIcon())
        assertEquals(DtcRowIcon.PENDING, dtcStatus(0x04).rowIcon())
        assertEquals(DtcRowIcon.HISTORY, dtcStatus(0x20).rowIcon())
        assertEquals(DtcRowIcon.FAULT, dtcStatus(0x00).rowIcon())
        assertEquals(DtcRowIcon.PERMANENT, dtcStatus(0x00).copy(permanent = true).rowIcon())
        assertEquals(DtcRowIcon.STORED, dtcStatus(0x08).copy(permanent = true).rowIcon())
    }
}
