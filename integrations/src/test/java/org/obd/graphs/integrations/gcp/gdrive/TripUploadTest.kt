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

class TripUploadTest {

    @Test
    fun `driveTripFileName strips the trip-profile_ prefix and appends json gz`() {
        assertEquals(
            "device123-1-1234567890-120.jsonl.json.gz",
            TripUpload.driveTripFileName("device123", "trip-profile_1-1234567890-120.jsonl")
        )
    }

    @Test
    fun `driveTripFileName leaves the name untouched if the prefix is absent`() {
        assertEquals(
            "device123-some-other-name.json.gz",
            TripUpload.driveTripFileName("device123", "some-other-name")
        )
    }
}
