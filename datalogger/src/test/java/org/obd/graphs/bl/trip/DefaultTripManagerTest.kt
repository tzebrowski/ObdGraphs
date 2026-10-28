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

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.TestSetup
import org.obd.graphs.profile.Profile
import org.obd.graphs.profile.profile
import org.obd.graphs.runAsync
import org.obd.graphs.sendBroadcastEvent
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.Serializable

private const val PROFILE = "profile_5"

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DefaultTripManagerTest : TestSetup() {

    private class RecordingRepository : TripRepository {
        val calls = mutableListOf<String>()

        override fun initStorage(tripId: String) {
            calls.add("init $tripId")
        }

        override fun releaseStorage(tripId: String) {
            calls.add("release $tripId")
        }

        override fun saveMetric(metric: Metric) {}

        override fun updateTripMetadata(tripId: String, tripStartTs: Long, tripLength: Long, profile: String) {
            calls.add("finish $tripId $tripStartTs")
        }

        override fun deleteTrip(tripId: String) {
            calls.add("delete $tripId")
        }

        override fun recoverUnfinishedTrips(): List<String> {
            calls.add("recover")
            return emptyList()
        }

        override fun findAllTripsBy(filter: String, profile: String): MutableCollection<TripFileDesc> = mutableListOf()

        override fun loadTrip(tripId: String, onMetricLoaded: (Metric) -> Unit) {}
    }

    private val repository = RecordingRepository()
    private val deferred = mutableListOf<() -> Any?>()

    @Before
    override fun setup() {
        super.setup()
        mockkStatic("org.obd.graphs.profile.ProfileKt")
        val currentProfile = mockk<Profile>()
        every { currentProfile.getCurrentProfile() } returns PROFILE
        every { profile } returns currentProfile

        mockkStatic("org.obd.graphs.BroadcastKt")
        every { sendBroadcastEvent(any<String>()) } just Runs
        every { sendBroadcastEvent(any(), any<Serializable>()) } just Runs

        // Async work runs only when the test says so, so a stop followed at once by a start can be replayed.
        mockkStatic("org.obd.graphs.AsyncKt")
        every { runAsync<Any?>(any(), any()) } answers {
            deferred.add(secondArg())
            null
        }
    }

    @After
    fun tearDown() = unmockkAll()

    private fun runDeferred() {
        while (deferred.isNotEmpty()) deferred.removeAt(0)()
    }

    private fun tripId(startTs: Long) = "trip-$PROFILE-$startTs.jsonl"

    @Test
    fun `a trip stopped and at once followed by the next one is saved under its own id`() {
        val manager = DefaultTripManager { repository }
        val first = System.currentTimeMillis() - 60_000
        manager.startNewTrip(first)

        manager.saveCurrentTrip()
        manager.startNewTrip(first + 30_000)
        runDeferred()

        assertEquals(
            listOf("release ${tripId(first)}", "finish ${tripId(first)} $first"),
            repository.calls.filter { it.startsWith("release") || it.startsWith("finish") }
        )
    }

    @Test
    fun `a trip still open when the next one starts is saved first`() {
        val manager = DefaultTripManager { repository }
        val first = System.currentTimeMillis() - 60_000
        manager.startNewTrip(first)

        manager.startNewTrip(first + 30_000)

        assertEquals(
            listOf(
                "recover",
                "init ${tripId(first)}",
                "release ${tripId(first)}",
                "finish ${tripId(first)} $first",
                "recover",
                "init ${tripId(first + 30_000)}"
            ),
            repository.calls
        )
    }
}
