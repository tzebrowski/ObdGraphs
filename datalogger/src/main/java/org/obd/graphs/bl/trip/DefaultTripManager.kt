/**
 * Copyright 2019-2023, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.obd.graphs.bl.trip

import android.content.Context
import android.util.Log
import com.github.mikephil.charting.data.Entry
import org.obd.graphs.MetricsProcessor
import org.obd.graphs.ValueScaler
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.getContext
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.isEnabled
import org.obd.graphs.profile.profile
import org.obd.metrics.api.model.ObdMetric
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

val tripManager: TripManager = DefaultTripManager()

private const val LOGGER_TAG = "TripManager"
private const val MIN_TRIP_LENGTH = 5
private const val TRIP_DIRECTORY = "trips"

private const val TRIP_FILE_PREFIX = "trip"

internal class DefaultTripManager : TripManager, MetricsProcessor {

    private val valueScaler = ValueScaler()

    private val dateFormat: SimpleDateFormat =
        SimpleDateFormat("MM.dd HH:mm:ss", Locale.getDefault())

    private val tripModelSerializer = TripModelSerializer()
    private val tripCache = TripCache()

    override fun postValue(metric: ObdMetric) {
        try {
            tripCache.getTrip { trip ->
                val ts = (System.currentTimeMillis() - trip.startTs).toFloat()
                val key = metric.command.pid.id
                val newRecord =
                    Entry(ts, valueScaler.scaleToNewRange(metric), key)

                if (trip.entries.containsKey(key)) {
                    val tripEntry = trip.entries[key]!!
                    tripEntry.metrics.add(
                        Metric(
                            entry = newRecord,
                            ts = metric.timestamp,
                            rawAnswer = metric.raw
                        )
                    )
                } else {
                    trip.entries[key] = SensorData(
                        id = key,
                        metrics = mutableListOf(
                            Metric(
                                entry = newRecord,
                                ts = metric.timestamp,
                                rawAnswer = metric.raw
                            )
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            Log.e(LOGGER_TAG, "Failed to add cache entry", e)
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
    }

    override fun saveCurrentTrip(f: () -> Unit) {
        tripCache.getTrip { trip ->
            val recordShortTrip = Prefs.isEnabled("pref.trips.recordings.save.short.trip")
            val tripLength = getTripLength(trip)
            Log.i(LOGGER_TAG, "Recorded trip, length: ${tripLength}s")

            if (recordShortTrip || tripLength > MIN_TRIP_LENGTH) {
                val tripStartTs = trip.startTs

                val filter = "$TRIP_FILE_PREFIX-${profile.getCurrentProfile()}-${tripStartTs}"
                val alreadySaved = findAllTripsBy(filter)

                if (alreadySaved.isNotEmpty()) {
                    Log.e(
                        LOGGER_TAG,
                        "It seems that Trip which start same date='${filter}' is already saved."
                    )
                } else {
                    try {
                        f()
                        val histogram = dataLogger.getDiagnostics().histogram()
                        val pidDefinitionRegistry = dataLogger.getPidDefinitionRegistry()

                        trip.entries.forEach { (t, u) ->
                            val p = pidDefinitionRegistry.findBy(t)
                            p?.let {
                                val histogramSupplier = histogram.findBy(it)
                                u.max = histogramSupplier.max
                                u.min = histogramSupplier.min
                                u.mean = histogramSupplier.mean
                            }
                        }

                        val content: String =
                            tripModelSerializer.serializer.writeValueAsString(trip)

                        val fileName =
                            "$TRIP_FILE_PREFIX-${profile.getCurrentProfile()}-${tripStartTs}-${tripLength}.json"
                        Log.i(
                            LOGGER_TAG,
                            "Saving the trip to the file: '$fileName'. Length: ${tripLength}s"
                        )
                        writeFile(getContext()!!, fileName, content)
                        Log.i(
                            LOGGER_TAG,
                            "Trip was written to the file: '$fileName'. Length: ${tripLength}s"
                        )
                    }catch (e: java.lang.Exception) {
                        Log.e(LOGGER_TAG,"Failed to save trip", e)
                    }
                }
            } else {
                Log.w(LOGGER_TAG, "Trip was not saved. Trip time is less than ${MIN_TRIP_LENGTH}s")
            }
        }
    }

    override fun findAllTripsBy(filter: String): MutableCollection<TripFileDesc> {
        Log.i(LOGGER_TAG, "Finds all trips by filter: '$filter' and profile=${profile.getCurrentProfile()}")

        val profiles = profile.getAvailableProfiles()
        val files = File(getTripsDirectory(getContext()!!)).list()
        if (files == null) {
            Log.i(LOGGER_TAG, "No files were found in the trips directory.")
            return mutableListOf()
        } else {
            val result = files
                .filter { if (filter.isNotEmpty()) it.startsWith(filter) else true }
                .filter { it.startsWith("${TRIP_FILE_PREFIX}_") || it.startsWith("$TRIP_FILE_PREFIX-") }
                .filter {
                    it.contains("${profile.getCurrentProfile()}-") }
                .filter {
                    try {
                        decodeTripName(it).size > 3
                    }catch (e: Throwable){
                        false
                    }
                }
                .mapNotNull { fileName ->
                    Log.d(LOGGER_TAG,"Found trip which fits the conditions: $fileName")
                    val p = decodeTripName(fileName)
                    val profileId = p[1]
                    val profileLabel = profiles[profileId]!!

                    TripFileDesc(
                        fileName = fileName,
                        profileId = profileId,
                        profileLabel = profileLabel,
                        startTime = p[2],
                        tripTimeSec = p[3]
                    )
                }
                .sortedByDescending { it.startTime.toLongOrNull() }
                .toMutableList()
            Log.i(LOGGER_TAG, "Found trips by filter: '${filter}' for profile=${profile.getCurrentProfile()}. Result size: ${result.size}")
            return result
        }
    }

    private fun decodeTripName(fileName: String) = fileName.substring(0, fileName.length - 5).split("-")

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
                val trip: Trip = tripModelSerializer.deserializer.readValue(file, Trip::class.java)
                Log.i(LOGGER_TAG, "Trip '${file.absolutePath}' was loaded from the disk.")
                tripCache.updateTrip(trip)
            } catch (e: FileNotFoundException) {
                Log.e(LOGGER_TAG, "Did not find trip '$tripName'.", e)
                updateCache(System.currentTimeMillis())
            }
        }
    }

    private fun writeFile(
        context: Context,
        fileName: String,
        content: String
    ) {

        var fd: FileOutputStream? = null
        try {
            val file = getTripFile(context, fileName)
            fd = FileOutputStream(file).apply {
                write(content.toByteArray())
            }

        } finally {
            fd?.run {
                flush()
                close()
            }
        }
    }

    private fun getTripFile(context: Context, fileName: String): File =
        File(getTripsDirectory(context), fileName)

    private fun getTripsDirectory(context: Context) =
        "${context.getExternalFilesDir(TRIP_DIRECTORY)?.absolutePath}"

    private fun updateCache(newTs: Long) {
        val trip = Trip(startTs = newTs, entries = mutableMapOf())
        tripCache.updateTrip(trip)
        Log.i(LOGGER_TAG, "Init new Trip with timestamp: '${formatTimestamp(newTs)}'")
    }

    private fun getTripLength(trip: Trip): Long = if (trip.startTs == 0L) 0 else {
        (Date().time - trip.startTs) / 1000
    }

    private fun formatTimestamp(ts: Long) =
        dateFormat.format(Date(ts))
}