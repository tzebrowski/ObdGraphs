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
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

private const val LOG_TAG = "TripRepository"
private const val TRIP_DIRECTORY = "trips"
private const val TRIP_FILE_PREFIX = "trip"
private const val TRIP_FILE_EXTENSION = ".jsonl"

// How much of an unfinished trip's end is read to find its last complete metric (a line is ~80 bytes).
private const val RECOVERY_TAIL_BYTES = 256 * 1024L

internal fun finalTripName(profile: String, tripStartTs: Long, tripLength: Long) =
    "$TRIP_FILE_PREFIX-$profile-$tripStartTs-$tripLength$TRIP_FILE_EXTENSION"

interface TripRepository {
    fun initStorage(tripId: String)
    fun releaseStorage(tripId: String)

    fun saveMetric(metric: Metric)
    fun updateTripMetadata(tripId: String, tripStartTs: Long, tripLength: Long, profile: String)
    fun deleteTrip(tripId: String)

    // Renames trips left with their temporary name (`trip-<profile>-<startTs>.jsonl`, no length) by a
    // recording that never reached updateTripMetadata(): the app was killed, crashed, or lost power.
    // The length comes from the last complete metric; a half-written last line is cut off. The trip
    // being recorded is never touched. Returns the new names.
    fun recoverUnfinishedTrips(): List<String>

    fun findAllTripsBy(filter: String, profile: String): MutableCollection<TripFileDesc>
    fun loadTrip(tripId: String, onMetricLoaded: (Metric) -> Unit)
}

internal class FileTripRepository(
    private val context: Context,
    private val parser: TripDescParser = TripDescParser(),
    private val serializer: TripModelSerializer = TripModelSerializer()
) : TripRepository {

    private var activeFileOutputStream: FileOutputStream? = null

    // The trip the open stream writes to; touched on the repository's thread only.
    private var activeFileTripId: String? = null

    @Volatile
    private var activeTripId: String? = null
    private val totalMetricsSaved = AtomicLong(0)

    private val ioDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val ioScope = CoroutineScope(ioDispatcher)

    private fun getTripsDirectory() = "${context.getExternalFilesDir(TRIP_DIRECTORY)?.absolutePath}"
    private fun getTripFile(fileName: String): File = File(getTripsDirectory(), fileName)

    override fun initStorage(tripId: String) {
        activeTripId = tripId
        totalMetricsSaved.set(0)

        ioScope.launch {
            try {
                // A stream left open by a trip that was never released would leak and stop it being flushed.
                activeFileOutputStream?.close()
                val file = getTripFile(tripId)
                activeFileOutputStream = FileOutputStream(file, true)
                activeFileTripId = tripId
                Log.i(LOG_TAG, "Started saving trip to: $activeTripId")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to initialize storage for trip", e)
            }
        }
    }

    override fun releaseStorage(tripId: String) {
        ioScope.launch {
            try {
                // A stopped trip is released asynchronously; on a quick reconnect the next trip has opened
                // its file by then, and closing that stream left the next trip empty (0 bytes).
                if (activeFileTripId != tripId) {
                    Log.i(LOG_TAG, "Trip '$tripId' is no longer written, not closing the stream of '$activeFileTripId'")
                    return@launch
                }
                activeFileOutputStream?.flush()
                activeFileOutputStream?.close()
                activeFileOutputStream = null
                activeFileTripId = null
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to release storage", e)
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
                Log.e(LOG_TAG, "Failed to save metric", e)
            }
        }
    }

    override fun updateTripMetadata(tripId: String, tripStartTs: Long, tripLength: Long, profile: String) {
        ioScope.launch {
            try {
                val currentFile = getTripFile(tripId)
                if (currentFile.exists()) {
                    val finalName = finalTripName(profile, tripStartTs, tripLength)
                    val finalFile = getTripFile(finalName)

                    val isRenamed = currentFile.renameTo(finalFile)

                    if (isRenamed) {
                        val totalItems = totalMetricsSaved.get()
                        val fileSizeMb = finalFile.length() / (1024.0 * 1024.0)
                        Log.i(LOG_TAG, "Trip finished. ID: '$finalName' | Saved: $totalItems metrics | Size: ${String.format("%.2f", fileSizeMb)} MB")
                    } else {
                        Log.e(LOG_TAG, "Failed to rename temporary trip file '$tripId' to '$finalName'.")
                    }
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to update trip metadata", e)
            } finally {
                // A trip started since then is still being recorded: clearing its id would let
                // recoverUnfinishedTrips() rename the file under it.
                if (activeTripId == tripId) activeTripId = null
            }
        }
    }

    override fun deleteTrip(tripId: String) {
        ioScope.launch {
            try {
                getTripFile(tripId).delete()
                Log.i(LOG_TAG, "Deleted trip data: $tripId")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to delete trip data", e)
            } finally {
                if (activeTripId == tripId) activeTripId = null
            }
        }
    }

    // Runs on the repository's own thread, after any queued write, release or rename.
    override fun recoverUnfinishedTrips(): List<String> =
        runBlocking(ioDispatcher) {
            val files = File(getTripsDirectory()).listFiles() ?: return@runBlocking emptyList()
            files.filter { isUnfinishedTrip(it.name) }.mapNotNull { recoverTrip(it) }
        }

    private fun isUnfinishedTrip(fileName: String): Boolean {
        if (!fileName.startsWith("$TRIP_FILE_PREFIX-") || !fileName.endsWith(TRIP_FILE_EXTENSION)) return false
        if (fileName == activeTripId) return false
        val parts = parser.decodeTripName(fileName)
        return parts.size == 3 && parts[2].toLongOrNull() != null
    }

    private fun recoverTrip(file: File): String? =
        try {
            val last = lastCompleteMetric(file)
            if (last == null) {
                Log.w(LOG_TAG, "Unfinished trip '${file.name}' holds no complete metric, leaving it as is.")
                null
            } else {
                val (metric, end) = last
                if (end < file.length()) {
                    RandomAccessFile(file, "rw").use { it.setLength(end) }
                }
                val parts = parser.decodeTripName(file.name)
                val finalName = finalTripName(parts[1], parts[2].toLong(), (metric.entry.x / 1000).toLong())
                val finalFile = getTripFile(finalName)
                if (!finalFile.exists() && file.renameTo(finalFile)) {
                    Log.i(LOG_TAG, "Recovered unfinished trip '${file.name}' as '$finalName'")
                    finalName
                } else {
                    Log.e(LOG_TAG, "Failed to rename unfinished trip '${file.name}' to '$finalName'.")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to recover unfinished trip '${file.name}'", e)
            null
        }

    // The last line of the file that parses as a metric, with the file offset just past it.
    private fun lastCompleteMetric(file: File): Pair<Metric, Long>? {
        val length = file.length()
        if (length == 0L) return null
        val windowStart = maxOf(0L, length - RECOVERY_TAIL_BYTES)
        val tail = ByteArray((length - windowStart).toInt())
        RandomAccessFile(file, "r").use {
            it.seek(windowStart)
            it.readFully(tail)
        }

        var lineEnd = tail.size
        while (lineEnd > 0) {
            val contentEnd = if (tail[lineEnd - 1] == '\n'.code.toByte()) lineEnd - 1 else lineEnd
            var lineStart = contentEnd
            while (lineStart > 0 && tail[lineStart - 1] != '\n'.code.toByte()) lineStart--
            // A line reaching the start of a partial window may itself be cut.
            if (lineStart == 0 && windowStart > 0) return null

            val line = String(tail, lineStart, contentEnd - lineStart, Charsets.UTF_8)
            if (line.isNotBlank()) {
                val metric =
                    try {
                        serializer.deserializer.readValue(line, Metric::class.java)
                    } catch (_: Exception) {
                        null
                    }
                if (metric != null) return metric to windowStart + lineEnd
            }
            lineEnd = lineStart
        }
        return null
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
                } catch (_: Throwable) {
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
