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
package org.obd.graphs.integrations.gcp.gdrive

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TripTagsTest {

    @Test
    fun `parse returns empty list for null input`() {
        assertTrue(TripTags.parse(null).isEmpty())
    }

    @Test
    fun `parse returns empty list for blank input`() {
        assertTrue(TripTags.parse("").isEmpty())
        assertTrue(TripTags.parse("   ").isEmpty())
    }

    @Test
    fun `parse splits comma-separated tags and trims whitespace`() {
        assertEquals(listOf("commute", "morning"), TripTags.parse("commute, morning"))
        assertEquals(listOf("track day"), TripTags.parse("  track day  "))
    }

    @Test
    fun `parse drops empty entries from stray commas`() {
        assertEquals(listOf("commute", "morning"), TripTags.parse("commute,,morning,"))
        assertEquals(listOf("commute"), TripTags.parse(",commute,"))
    }

    @Test
    fun `format joins tags with a comma and no spaces`() {
        assertEquals("commute,morning", TripTags.format(listOf("commute", "morning")))
        assertEquals("track day", TripTags.format(listOf("track day")))
        assertEquals("", TripTags.format(emptyList()))
    }

    @Test
    fun `format then parse round-trips to the same tags`() {
        val tags = listOf("2.0gme", "bluetooth", "stn")
        assertEquals(tags, TripTags.parse(TripTags.format(tags)))
    }
}
