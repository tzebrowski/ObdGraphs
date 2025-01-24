 /**
 * Copyright 2019-2025, Tomasz Żebrowski
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

import android.util.Log
import org.obd.graphs.*
import org.obd.graphs.bl.datalogger.MetricsProcessor
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.commons.R

private const val LOG_TAG = "TripManager"

interface TripManager : MetricsProcessor {
    fun getCurrentTrip(): Trip
    fun startNewTrip(newTs: Long)
    fun saveCurrentTrip(f: () -> Unit)

    fun saveCurrentTripAsync(){

        sendBroadcastEvent(SCREEN_LOCK_PROGRESS_EVENT, getContext()?.getText(R.string.dialog_screen_lock_trip_save_message) as String)

        runAsync (wait = false) {
            try {
                tripManager.saveCurrentTrip {}
            } finally {
                sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT)
            }
        }
    }

    fun findAllTripsBy(filter: String = ""): MutableCollection<TripFileDesc>
    fun deleteTrip(trip: TripFileDesc)
    fun loadTrip(tripName: String)
    fun loadTripAsync(tripName: String){
        if (!dataLogger.isRunning()) {
            sendBroadcastEvent(SCREEN_LOCK_PROGRESS_EVENT, getContext()?.getText(R.string.dialog_screen_lock_trip_load_message) as String)
            runAsync (wait = false) {
                try {

                    Log.i(LOG_TAG, "Loading trip: '$tripName' ...................")
                    tripManager.loadTrip(tripName)
                    Log.i(LOG_TAG, "Trip: '$tripName' is loaded")
                } finally {
                    sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT)
                }
            }
        }
    }

}
