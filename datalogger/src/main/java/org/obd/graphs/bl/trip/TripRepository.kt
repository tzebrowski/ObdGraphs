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

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

private const val LOGGER_TAG = "TripRepository"
private const val TRIP_DIRECTORY = "trips"
private const val TRIP_FILE_PREFIX = "trip"

interface TripRepository {
    fun initStorage(tripId: String)
    fun releaseStorage(tripId: String)

    fun saveMetric(metric: Metric)
    fun updateTripMetadata(tripId: String, tripStartTs: Long, tripLength: Long, profile: String)
    fun deleteTrip(tripId: String)

    fun findAllTripsBy(filter: String, profile: String): MutableCollection<TripFileDesc>
    fun loadTrip(tripId: String, onMetricLoaded: (Metric) -> Unit)
}

internal class FileTripRepository(
    private val context: Context,
    private val parser: TripDescParser = TripDescParser(),
    private val serializer: TripModelSerializer = TripModelSerializer()
) : TripRepository {

    private var activeFileOutputStream: FileOutputStream? = null
    private var activeTripId: String? = null
    private val totalMetricsSaved = AtomicLong(0)

    // Single thread dispatcher guarantees sequential disk I/O
    private val ioDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val ioScope = CoroutineScope(ioDispatcher)

    private fun getTripsDirectory() = "${context.getExternalFilesDir(TRIP_DIRECTORY)?.absolutePath}"
    private fun getTripFile(fileName: String): File = File(getTripsDirectory(), fileName)

    override fun initStorage(tripId: String) {
        activeTripId = tripId
        totalMetricsSaved.set(0)

        ioScope.launch {
            try {
                val file = getTripFile(activeTripId!!)
                activeFileOutputStream = FileOutputStream(file, true)
                Log.i(LOGGER_TAG, "Started saving trip to: $activeTripId")
            } catch (e: Exception) {
                Log.e(LOGGER_TAG, "Failed to initialize storage for trip", e)
            }
        }
    }

    override fun releaseStorage(tripId: String) {
        ioScope.launch {
            try {
                activeFileOutputStream?.flush()
                activeFileOutputStream?.close()
                activeFileOutputStream = null
            } catch (e: Exception) {
                Log.e(LOGGER_TAG, "Failed to release storage", e)
            }
        }
    }

    override fun saveMetric(metric: Metric) {
        ioScope.launch {
            try {
                activeFileOutputStream?.let {
                    val jsonLine = serializer.serializer.writeValueAsString(metric) + "\n"
                    it.write(jsonLine.toByteArray())
                    totalMetricsSaved.incrementAndGet()
                }
            } catch (e: Exception) {
                Log.e(LOGGER_TAG, "Failed to save metric", e)
            }
        }
    }

    override fun updateTripMetadata(tripId: String, tripStartTs: Long, tripLength: Long, profile: String) {
        ioScope.launch {
            try {
                val currentFile = getTripFile(tripId)
                if (currentFile.exists()) {
                    val finalName = "$TRIP_FILE_PREFIX-$profile-$tripStartTs-$tripLength.jsonl"
                    val finalFile = getTripFile(finalName)
                    currentFile.renameTo(finalFile)

                    val totalItems = totalMetricsSaved.get()
                    val fileSizeMb = finalFile.length() / (1024.0 * 1024.0)
                    Log.i(LOGGER_TAG, "Trip finished. ID: '$finalName' | Saved: $totalItems metrics | Size: ${String.format("%.2f", fileSizeMb)} MB")
                }
            } catch (e: Exception) {
                Log.e(LOGGER_TAG, "Failed to update trip metadata", e)
            } finally {
                activeTripId = null
            }
        }
    }

    override fun deleteTrip(tripId: String) {
        ioScope.launch {
            try {
                getTripFile(tripId).delete()
                Log.i(LOGGER_TAG, "Deleted trip data: $tripId")
            } catch (e: Exception) {
                Log.e(LOGGER_TAG, "Failed to delete trip data", e)
            } finally {
                if (activeTripId == tripId) activeTripId = null
            }
        }
    }

    override fun findAllTripsBy(filter: String, profile: String): MutableCollection<TripFileDesc> {
        val files = File(getTripsDirectory()).list() ?: return mutableListOf()

        return files
            .filter { if (filter.isNotEmpty()) it.startsWith(filter) else true }
            .filter { it.startsWith("${TRIP_FILE_PREFIX}_") || it.startsWith("$TRIP_FILE_PREFIX-") }
            .filter { it.contains("$profile-") }
            .filter {
                try {
                    parser.decodeTripName(it).size > 3
                } catch (e: Throwable) {
                    false
                }
            }.mapNotNull { fileName ->
                parser.getTripDesc(fileName)
            }.sortedByDescending { it.startTime.toLongOrNull() }
            .toMutableList()
    }

    override fun loadTrip(tripId: String, onMetricLoaded: (Metric) -> Unit) {
        val file = getTripFile(tripId)
        if (file.exists()) {
            file.forEachLine { line ->
                if (line.isNotBlank()) {
                    val metric = serializer.deserializer.readValue(line, Metric::class.java)
                    onMetricLoaded(metric)
                }
            }
        } else {
            throw IllegalArgumentException("Trip data not found for ID: $tripId")
        }
    }
}
