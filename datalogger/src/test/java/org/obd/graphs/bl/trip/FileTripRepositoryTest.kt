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
package org.obd.graphs.bl.trip

import io.mockk.every
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.TestSetup
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FileTripRepositoryTest : TestSetup() {

    private lateinit var tripsDir: File
    private lateinit var repository: FileTripRepository

    @Before
    override fun setup() {
        super.setup()
        tripsDir = Files.createTempDirectory("trips-test").toFile()
        every { context.getExternalFilesDir(any()) } returns tripsDir

        repository = FileTripRepository(context, parser = TripDescParser(), serializer = TripModelSerializer())
    }

    @After
    fun tearDown() {
        tripsDir.deleteRecursively()
        unmockkAll()
    }

    private fun createTripFile(name: String) {
        File(tripsDir, name).createNewFile()
    }

    @Test
    fun `findAllTripsBy returns an empty collection when the trips directory does not exist`() {
        every { context.getExternalFilesDir(any()) } returns File(tripsDir, "does-not-exist")

        val result = FileTripRepository(context, parser = TripDescParser(), serializer = TripModelSerializer())
            .findAllTripsBy(filter = "", profile = "profileA")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findAllTripsBy returns only trips matching the requested profile, sorted by newest first`() {
        createTripFile("trip-profileA-1700000000-120.jsonl")
        createTripFile("trip-profileA-1750000000-60.jsonl")
        createTripFile("trip-profileB-1700000005-30.jsonl")

        val result = repository.findAllTripsBy(filter = "", profile = "profileA")

        assertEquals(
            listOf("trip-profileA-1750000000-60.jsonl", "trip-profileA-1700000000-120.jsonl"),
            result.map { it.fileName }
        )
    }

    @Test
    fun `findAllTripsBy excludes files that do not carry the trip prefix`() {
        createTripFile("trip-profileA-1700000000-120.jsonl")
        createTripFile("not_a_trip_file.txt")

        val result = repository.findAllTripsBy(filter = "", profile = "profileA")

        assertEquals(listOf("trip-profileA-1700000000-120.jsonl"), result.map { it.fileName })
    }

    @Test
    fun `findAllTripsBy excludes file names that do not decode into enough segments`() {
        createTripFile("trip-profileA-1700000000-120.jsonl")
        // Only 3 dash-separated segments after stripping the extension: trip, profileA, badname.
        createTripFile("trip-profileA-badname.jsonl")

        val result = repository.findAllTripsBy(filter = "", profile = "profileA")

        assertEquals(listOf("trip-profileA-1700000000-120.jsonl"), result.map { it.fileName })
    }

    @Test
    fun `findAllTripsBy applies a non-empty filter as a filename prefix`() {
        createTripFile("trip-profileA-1700000000-120.jsonl")
        createTripFile("trip-profileA-1750000000-60.jsonl")

        val result = repository.findAllTripsBy(filter = "trip-profileA-1750000000", profile = "profileA")

        assertEquals(listOf("trip-profileA-1750000000-60.jsonl"), result.map { it.fileName })
    }
}
