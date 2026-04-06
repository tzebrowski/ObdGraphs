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
import org.obd.graphs.SCREEN_LOCK_PROGRESS_EVENT
import org.obd.graphs.SCREEN_UNLOCK_PROGRESS_EVENT
import org.obd.graphs.ScreenLock
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.scaleToRange
import org.obd.graphs.commons.R
import org.obd.graphs.getContext
import org.obd.graphs.isNumber
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.isEnabled
import org.obd.graphs.profile.profile
import org.obd.graphs.runAsync
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.api.model.ObdMetric
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val tripManager: TripManager by lazy { DefaultTripManager() }

private const val LOG_TAG = "TripManager"
private const val MIN_TRIP_LENGTH = 5
private const val TRIP_FILE_PREFIX = "trip"

private const val MAX_CACHED_METRICS_PER_SENSOR = 18000

internal class DefaultTripManager : TripManager {

    private val dateFormat: SimpleDateFormat = SimpleDateFormat("MM.dd HH:mm:ss", Locale.getDefault())
    private val tripCache = TripCache()
    private val tripDescParser = TripDescParser()

    private val repository: TripRepository by lazy { FileTripRepository(getContext()!!) }

    private var activeTripId: String? = null

    override fun getTripsDirectory(context: Context) = "${context.getExternalFilesDir("trips")?.absolutePath}"

    override fun postValue(obdMetric: ObdMetric) {
        try {
            tripCache.getTrip { trip ->
                val ts = (System.currentTimeMillis() - trip.startTs).toFloat()
                val id = obdMetric.command.pid.id
                val newRecord = if (obdMetric.isNumber()) Entry(ts, obdMetric.scaleToRange(), id) else Entry(ts, obdMetric.value, id)

                val metric = Metric(
                    entry = newRecord,
                    ts = obdMetric.timestamp,
                    rawAnswer = obdMetric.raw
                )

                repository.saveMetric(metric)

                val tripEntry = trip.entries.getOrPut(id) {
                    SensorData(id = id)
                }

                synchronized(tripEntry.metrics) {
                    tripEntry.metrics.add(metric)

                    while (tripEntry.metrics.size > MAX_CACHED_METRICS_PER_SENSOR) {
                        tripEntry.metrics.removeFirst()
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to process metric for ${obdMetric.command.pid.pid}", e)
        }
    }

    override fun getCurrentTrip(): Trip {
        val trip = tripCache.getTrip() ?: run {
            val newTs = System.currentTimeMillis()
            startNewTrip(newTs)
            tripCache.getTrip() ?: Trip(startTs = newTs)
        }

        Log.i(LOG_TAG, "Get current trip ts: '${formatTimestamp(trip.startTs)}'")
        return trip
    }

    override fun startNewTrip(newTs: Long) {
        Log.i(LOG_TAG, "Starting new trip, timestamp: '${formatTimestamp(newTs)}'")
        updateCache(newTs)

        activeTripId = "$TRIP_FILE_PREFIX-${profile.getCurrentProfile()}-$newTs.jsonl"
        repository.initStorage(activeTripId!!)
    }

    override fun loadTrip(tripName: String) {
        if (!DataLoggerRepository.isRunning()) {
            runAsync(wait = false) {
                try {
                    Log.i(LOG_TAG, "Loading trip: '$tripName' ...................")

                    if (tripName.isEmpty()) {
                        updateCache(System.currentTimeMillis())
                    } else {
                        sendBroadcastEvent(
                            SCREEN_LOCK_PROGRESS_EVENT,
                            ScreenLock(message = R.string.dialog_screen_lock_trip_load_message)
                        )

                        try {
                            val parts = tripDescParser.decodeTripName(tripName)
                            val startTs = parts.getOrNull(2)?.toLongOrNull() ?: System.currentTimeMillis()
                            val trip = Trip(startTs = startTs)

                            repository.loadTrip(tripName) { metric ->
                                val key = metric.entry.data
                                if (!trip.entries.containsKey(key)) {
                                    trip.entries[key] = SensorData(id = key)
                                }
                                trip.entries[key]!!.metrics.add(metric)
                            }

                            trip.entries.values.forEach { sensorData ->
                                val values = sensorData.metrics.mapNotNull { it.entry.y.toString().toFloatOrNull() }
                                if (values.isNotEmpty()) {
                                    sensorData.min = values.minOrNull() ?: 0f
                                    sensorData.max = values.maxOrNull() ?: 0f
                                    sensorData.mean = values.average()
                                }
                            }

                            Log.i(LOG_TAG, "Trip loaded successfully. PIDs: ${trip.entries.keys}")

                            tripCache.updateTrip(trip)
                            tripVirtualScreenManager.updateReservedVirtualScreen(trip.entries.keys.map { it.toString() })

                            Log.i(LOG_TAG, "Trip: '$tripName' is loaded")
                        } catch (e: Throwable) {
                            Log.e(LOG_TAG, "Failed to load trip '$tripName'.", e)
                            updateCache(System.currentTimeMillis())
                        }
                    }
                } finally {
                    sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT)
                }
            }
        }
    }

    override fun saveCurrentTrip() {
        sendBroadcastEvent(
            SCREEN_LOCK_PROGRESS_EVENT,
            ScreenLock(message = R.string.dialog_screen_lock_trip_save_message)
        )

        runAsync(wait = false) {
            try {
                tripCache.getTrip { trip ->
                    var ts = System.currentTimeMillis()
                    val recordShortTrip = Prefs.isEnabled("pref.trips.recordings.save.short.trip")
                    val tripLength = getTripLength(trip)
                    val currentTripId = activeTripId ?: return@getTrip

                    Log.i(LOG_TAG, "Stopping current trip: $currentTripId, length: ${tripLength}s")

                    repository.releaseStorage(currentTripId)

                    if (recordShortTrip || tripLength > MIN_TRIP_LENGTH) {
                        repository.updateTripMetadata(currentTripId, trip.startTs, tripLength, profile.getCurrentProfile())
                    } else {
                        Log.w(LOG_TAG, "Trip time is less than ${MIN_TRIP_LENGTH}s. Discarding.")
                        repository.deleteTrip(currentTripId)
                    }

                    activeTripId = null
                    ts = System.currentTimeMillis() - ts
                    Log.i(LOG_TAG, "Trip: $currentTripId is saved. It took $ts ms")
                }
            } finally {
                sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT)
            }
        }
    }

    override fun findAllTripsBy(filter: String): MutableCollection<TripFileDesc> {
        return repository.findAllTripsBy(filter, profile.getCurrentProfile())
    }

    override fun deleteTrip(trip: TripFileDesc) {
        repository.deleteTrip(trip.fileName)
    }

    private fun updateCache(newTs: Long) {
        val trip = Trip(startTs = newTs)
        tripCache.updateTrip(trip)
    }

    private fun getTripLength(trip: Trip): Long =
        if (trip.startTs == 0L) 0 else (Date().time - trip.startTs) / 1000

    private fun formatTimestamp(ts: Long) = dateFormat.format(Date(ts))
}
