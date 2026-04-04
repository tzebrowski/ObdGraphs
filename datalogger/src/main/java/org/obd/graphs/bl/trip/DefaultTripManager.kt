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
import org.obd.graphs.bl.datalogger.MetricsProcessor
import org.obd.graphs.bl.datalogger.scaleToRange
import org.obd.graphs.getContext
import org.obd.graphs.isNumber
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.isEnabled
import org.obd.graphs.profile.profile
import org.obd.metrics.api.model.ObdMetric
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

val tripManager: TripManager = DefaultTripManager()

private const val LOGGER_TAG = "TripManager"
private const val MIN_TRIP_LENGTH = 5
private const val TRIP_DIRECTORY = "trips"
private const val TRIP_FILE_PREFIX = "trip"

// Holds exactly 30 minutes of data per sensor at 10Hz
private const val MAX_CACHED_METRICS_PER_SENSOR = 18000

internal class DefaultTripManager :
    TripManager,
    MetricsProcessor {
    private val dateFormat: SimpleDateFormat =
        SimpleDateFormat("MM.dd HH:mm:ss", Locale.getDefault())

    private val tripModelSerializer = TripModelSerializer()
    private val tripCache = TripCache()
    private val tripDescParser = TripDescParser()

    // Properties for streaming
    private var activeFileOutputStream: FileOutputStream? = null
    private var activeTripFileName: String? = null

    // Single thread dispatcher for sequential, non-blocking file operations
    private val fileIoDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val fileIoScope = CoroutineScope(fileIoDispatcher)

    override fun getTripsDirectory(context: Context) = "${context.getExternalFilesDir(TRIP_DIRECTORY)?.absolutePath}"

    override fun postValue(obdMetric: ObdMetric) {
        try {
            tripCache.getTrip { trip ->
                val ts = (System.currentTimeMillis() - trip.startTs).toFloat()
                val key = obdMetric.command.pid.id
                val newRecord = if (obdMetric.isNumber()) Entry(ts, obdMetric.scaleToRange(), key) else Entry(ts, obdMetric.value, key)

                val metric = Metric(
                    entry = newRecord,
                    ts = obdMetric.timestamp,
                    rawAnswer = obdMetric.raw
                )

                // STREAM TO FILE (Sequential, Non-Blocking via single-thread dispatcher)
                fileIoScope.launch {
                    try {
                        val jsonLine = tripModelSerializer.serializer.writeValueAsString(metric) + "\n"
                        activeFileOutputStream?.write(jsonLine.toByteArray())
                    } catch (e: Exception) {
                        Log.e(LOGGER_TAG, "Failed to stream line to JSONL file", e)
                    }
                }

                // UPDATE RAM CACHE (With 30-min Cap)
                if (trip.entries.containsKey(key)) {
                    val tripEntry = trip.entries[key]!!
                    tripEntry.metrics.add(metric)

                    // Memory Protection: Cap the list size
                    if (tripEntry.metrics.size > MAX_CACHED_METRICS_PER_SENSOR) {
                        tripEntry.metrics.removeAt(0)
                    }
                } else {
                    trip.entries[key] =
                        SensorData(
                            id = key,
                            metrics = mutableListOf(metric)
                        )
                }
            }
        } catch (e: Throwable) {
            Log.e(LOGGER_TAG, "Failed to add cache entry for ${obdMetric.command.pid.pid}", e)
        }
    }

    override fun getCurrentTrip(): Trip {
        if (null == tripCache.getTrip()) {
            startNewTrip(System.currentTimeMillis())
        }

        val trip = tripCache.getTrip()!!
        Log.i(LOGGER_TAG, "Get current trip ts: '${formatTimestamp(trip.startTs)}'")
        return trip
    }

    override fun startNewTrip(newTs: Long) {
        Log.i(LOGGER_TAG, "Starting new trip, timestamp: '${formatTimestamp(newTs)}'")
        updateCache(newTs)

        // Generate the file name
        val fileName = "$TRIP_FILE_PREFIX-${profile.getCurrentProfile()}-$newTs.jsonl"
        activeTripFileName = fileName

        // Open the file stream for appending on the sequential thread
        fileIoScope.launch {
            try {
                val file = getTripFile(getContext()!!, fileName)
                activeFileOutputStream = FileOutputStream(file, true)
                Log.i(LOGGER_TAG, "Opened stream for file: $fileName")
            } catch (e: Exception) {
                Log.e(LOGGER_TAG, "Failed to open file stream for streaming", e)
            }
        }
    }

    override fun saveCurrentTrip() {
        tripCache.getTrip { trip ->
            val recordShortTrip = Prefs.isEnabled("pref.trips.recordings.save.short.trip")
            val tripLength = getTripLength(trip)
            Log.i(LOGGER_TAG, "Stopping trip, length: ${tripLength}s")

            val fileNameToProcess = activeTripFileName

            if (recordShortTrip || tripLength > MIN_TRIP_LENGTH) {
                fileIoScope.launch {
                    try {
                        activeFileOutputStream?.flush()
                        activeFileOutputStream?.close()
                        activeFileOutputStream = null

                        fileNameToProcess?.let { currentName ->
                            val currentFile = getTripFile(getContext()!!, currentName)
                            if (currentFile.exists()) {
                                val finalName = "$TRIP_FILE_PREFIX-${profile.getCurrentProfile()}-${trip.startTs}-$tripLength.jsonl"
                                val finalFile = getTripFile(getContext()!!, finalName)
                                currentFile.renameTo(finalFile)
                                Log.i(LOGGER_TAG, "Trip stream closed and renamed to: '$finalName'")
                            }
                        }
                    } catch (e: java.lang.Exception) {
                        Log.e(LOGGER_TAG, "Failed to finalize streaming trip file", e)
                    } finally {
                        activeTripFileName = null
                    }
                }
            } else {
                Log.w(LOGGER_TAG, "Trip time is less than ${MIN_TRIP_LENGTH}s. Deleting short file.")
                fileIoScope.launch {
                    try {
                        activeFileOutputStream?.close()
                        activeFileOutputStream = null

                        fileNameToProcess?.let {
                            getTripFile(getContext()!!, it).delete()
                        }
                    } catch (e: Exception) {
                        Log.e(LOGGER_TAG, "Failed to delete short trip file", e)
                    } finally {
                        activeTripFileName = null
                    }
                }
            }
        }
    }

    override fun findAllTripsBy(filter: String): MutableCollection<TripFileDesc> {
        Log.i(LOGGER_TAG, "Finds all trips by filter: '$filter' and profile=${profile.getCurrentProfile()}")

        val files = File(getTripsDirectory(getContext()!!)).list()
        if (files == null) {
            Log.i(LOGGER_TAG, "No files were found in the trips directory.")
            return mutableListOf()
        } else {
            val result =
                files
                    .filter { if (filter.isNotEmpty()) it.startsWith(filter) else true }
                    .filter { it.startsWith("${TRIP_FILE_PREFIX}_") || it.startsWith("$TRIP_FILE_PREFIX-") }
                    .filter { it.contains("${profile.getCurrentProfile()}-") }
                    .filter {
                        try {
                            tripDescParser.decodeTripName(it).size > 3
                        } catch (e: Throwable) {
                            false
                        }
                    }.mapNotNull { fileName ->
                        Log.d(LOGGER_TAG, "Found trip which fits the conditions: $fileName")
                        tripDescParser.getTripDesc(fileName)
                    }.sortedByDescending { it.startTime.toLongOrNull() }
                    .toMutableList()
            Log.i(LOGGER_TAG, "Found trips by filter: '$filter' for profile=${profile.getCurrentProfile()}. Result size: ${result.size}")
            return result
        }
    }

    override fun deleteTrip(trip: TripFileDesc) {
        Log.i(LOGGER_TAG, "Deleting '${trip.fileName}' from the storage.")
        val file = File(getTripsDirectory(getContext()!!), trip.fileName)
        file.delete()
        Log.i(LOGGER_TAG, "Trip '${trip.fileName}' has been deleted from the storage.")
    }

    override fun loadTrip(tripName: String) {
        Log.i(LOGGER_TAG, "Loading '$tripName' from disk.")

        if (tripName.isEmpty()) {
            updateCache(System.currentTimeMillis())
        } else {
            val file = File(getTripsDirectory(getContext()!!), tripName)
            try {
                val parts = tripDescParser.decodeTripName(tripName)
                val startTs = parts[2].toLongOrNull() ?: System.currentTimeMillis()
                val trip = Trip(startTs = startTs)

                // Read line-by-line (JSONL) instead of as one massive object
                file.forEachLine { line ->
                    if (line.isNotBlank()) {
                        val metric = tripModelSerializer.deserializer.readValue(line, Metric::class.java)
                        val key = metric.entry.data

                        if (!trip.entries.containsKey(key)) {
                            trip.entries[key] = SensorData(id = key)
                        }

                        val sensorData = trip.entries[key]!!
                        sensorData.metrics.add(metric)
                    }
                }

                // Calculate historical min/max/mean from loaded values
                trip.entries.values.forEach { sensorData ->
                    val values = sensorData.metrics.mapNotNull { it.entry.y.toString().toFloatOrNull() }
                    if (values.isNotEmpty()) {
                        sensorData.min = values.minOrNull() ?: 0f
                        sensorData.max = values.maxOrNull() ?: 0f
                        sensorData.mean = values.average()
                    }
                }

                Log.i(LOGGER_TAG, "Trip '${file.absolutePath}' was loaded from the storage.")
                Log.i(LOGGER_TAG, "Trip selected PIDs ${trip.entries.keys}")
                Log.i(LOGGER_TAG, "Number of entries ${trip.entries.values.size} collected within the trip")

                tripCache.updateTrip(trip)
                tripVirtualScreenManager.updateReservedVirtualScreen(
                    trip.entries.keys.map { it.toString() }.toList()
                )
            } catch (e: Throwable) {
                Log.e(LOGGER_TAG, "Did not find or failed to parse trip '$tripName'.", e)
                updateCache(System.currentTimeMillis())
            }
        }
    }

    private fun getTripFile(
        context: Context,
        fileName: String
    ): File = File(getTripsDirectory(context), fileName)

    private fun updateCache(newTs: Long) {
        val trip = Trip(startTs = newTs)
        tripCache.updateTrip(trip)
        Log.i(LOGGER_TAG, "Init new Trip with timestamp: '${formatTimestamp(newTs)}'")
    }

    private fun getTripLength(trip: Trip): Long =
        if (trip.startTs == 0L) {
            0
        } else {
            (Date().time - trip.startTs) / 1000
        }

    private fun formatTimestamp(ts: Long) = dateFormat.format(Date(ts))
}
