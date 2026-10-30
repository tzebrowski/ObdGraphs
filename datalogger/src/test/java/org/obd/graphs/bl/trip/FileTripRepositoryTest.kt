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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.TestSetup
import org.obd.metrics.transport.message.ConnectorResponseFactory
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

    private fun metricLine(x: Float, pid: Long = 6004L): String =
        TripModelSerializer().serializer.writeValueAsString(
            Metric(entry = Entry(x = x, y = 100f, data = pid), ts = 1700000000L + x.toLong(), rawAnswer = ConnectorResponseFactory.wrap(byteArrayOf()))
        ) + "\n"

    private fun writeTrip(name: String, content: String): File = File(tripsDir, name).apply { writeText(content) }

    @Test
    fun `releasing a stopped trip after the next one started does not close the next trip's file`() {
        val metric = Metric(entry = Entry(x = 1f, y = 100f, data = 6004L), ts = 1700000001L, rawAnswer = ConnectorResponseFactory.wrap(byteArrayOf()))

        // A quick reconnect: the next trip opens its file before the stopped trip's async save releases it.
        repository.initStorage("trip-profileA-1700000000.jsonl")
        repository.initStorage("trip-profileA-1700000100.jsonl")
        repository.releaseStorage("trip-profileA-1700000000.jsonl")
        repository.saveMetric(metric)
        repository.releaseStorage("trip-profileA-1700000100.jsonl")
        // Runs on the repository's thread, so every write queued above has finished.
        repository.recoverUnfinishedTrips()

        assertEquals(1, File(tripsDir, "trip-profileA-1700000100.jsonl").readLines().size)
    }

    @Test
    fun `recoverUnfinishedTrips names a cut-off trip by its last metric and drops a half-written line`() {
        val complete = metricLine(1_000f) + metricLine(61_500f)
        writeTrip("trip-profileA-1700000000.jsonl", complete + "{\"entry\":{\"x\":62")

        val recovered = repository.recoverUnfinishedTrips()

        assertEquals(listOf("trip-profileA-1700000000-61.jsonl"), recovered)
        assertFalse(File(tripsDir, "trip-profileA-1700000000.jsonl").exists())
        assertEquals(complete, File(tripsDir, "trip-profileA-1700000000-61.jsonl").readText())

        val loaded = mutableListOf<Metric>()
        repository.loadTrip("trip-profileA-1700000000-61.jsonl") { loaded.add(it) }
        assertEquals(2, loaded.size)
        assertEquals(
            listOf("trip-profileA-1700000000-61.jsonl"),
            repository.findAllTripsBy(filter = "", profile = "profileA").map { it.fileName }
        )
    }

    @Test
    fun `recoverUnfinishedTrips keeps a trip whose last line is complete without a newline`() {
        val content = metricLine(1_000f) + metricLine(30_000f).trimEnd()
        writeTrip("trip-profileA-1700000000.jsonl", content)

        assertEquals(listOf("trip-profileA-1700000000-30.jsonl"), repository.recoverUnfinishedTrips())
        assertEquals(content, File(tripsDir, "trip-profileA-1700000000-30.jsonl").readText())
    }

    @Test
    fun `recoverUnfinishedTrips never touches the trip being recorded`() {
        writeTrip("trip-profileA-1700000000.jsonl", metricLine(1_000f))
        repository.initStorage("trip-profileA-1700000000.jsonl")

        assertTrue(repository.recoverUnfinishedTrips().isEmpty())
        assertTrue(File(tripsDir, "trip-profileA-1700000000.jsonl").exists())
    }

    @Test
    fun `finishing an earlier trip does not expose the one being recorded to recovery`() {
        writeTrip("trip-profileA-1700000000.jsonl", metricLine(1_000f))
        writeTrip("trip-profileA-1700000100.jsonl", metricLine(1_000f))
        repository.initStorage("trip-profileA-1700000000.jsonl")
        // A reconnect starts the next trip before the earlier one's rename has run.
        repository.initStorage("trip-profileA-1700000100.jsonl")
        repository.updateTripMetadata("trip-profileA-1700000000.jsonl", 1700000000L, 60, "profileA")

        assertTrue(repository.recoverUnfinishedTrips().isEmpty())
        assertTrue(File(tripsDir, "trip-profileA-1700000000-60.jsonl").exists())
        assertTrue(File(tripsDir, "trip-profileA-1700000100.jsonl").exists())
    }

    @Test
    fun `recoverUnfinishedTrips leaves files it cannot read a metric from and finished trips alone`() {
        writeTrip("trip-profileA-1700000000.jsonl", "{\"entry\":{\"x\":1")
        writeTrip("trip-profileA-1700000200.jsonl", "")
        writeTrip("trip-profileA-1700000100-60.jsonl", metricLine(1_000f))
        writeTrip("trip-profileA-1700000300-60.jsonl.synced", metricLine(1_000f))

        assertTrue(repository.recoverUnfinishedTrips().isEmpty())
        assertEquals(
            setOf(
                "trip-profileA-1700000000.jsonl",
                "trip-profileA-1700000200.jsonl",
                "trip-profileA-1700000100-60.jsonl",
                "trip-profileA-1700000300-60.jsonl.synced"
            ),
            tripsDir.list()!!.toSet()
        )
    }
}
