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

class MaxHeightListViewTest {
    @Test
    fun `the list keeps its share of the screen while the texts around it are short`() {
        assertEquals(800, pickerListMaxHeight(screenHeight = 2000, ratio = 0.4f, reserved = 500, minHeight = 250))
    }

    @Test
    fun `long texts above the list shrink it so the buttons stay on screen`() {
        // The Clear flow on a phone: warning, CAN header hint, UDS / OBD-II hint and buttons take 1000 px.
        assertEquals(600, pickerListMaxHeight(screenHeight = 2000, ratio = 0.4f, reserved = 1000, minHeight = 250))
    }

    @Test
    fun `the list never shrinks below its minimum`() {
        assertEquals(250, pickerListMaxHeight(screenHeight = 2000, ratio = 0.4f, reserved = 1700, minHeight = 250))
    }
}
