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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.obd.graphs.*
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTED_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_STOPPED_EVENT
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.commons.R


private const val LOGGER_KEY = "TripRecorderBroadcastReceiver"

class TripManagerBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {

            DATA_LOGGER_CONNECTED_EVENT -> {
                Log.i(LOGGER_KEY, "Received event: DATA_LOGGER_CONNECTED_EVENT")
                tripManager.startNewTrip(System.currentTimeMillis())
            }

            DATA_LOGGER_STOPPED_EVENT -> {
                Log.d(LOGGER_KEY, "Received data logger on stopped event. Saving trip into file.")

                runAsync {
                    try {
                        tripManager.saveCurrentTrip {
                            val msg = context?.getText(R.string.dialog_screen_lock_saving_trip_message) as String
                            sendBroadcastEvent(SCREEN_LOCK_PROGRESS_EVENT, msg)
                        }
                    } finally {
                        sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT)
                    }
                }
            }
            TRIP_LOAD_EVENT -> {

                if (!dataLogger.isRunning()) {
                    context?.run {
                        runAsync {
                            try {
                                val tripName = intent.getExtraParam()
                                Log.i(LOGGER_KEY, "Loading trip: '$tripName' ...................")
                                sendBroadcastEvent(SCREEN_LOCK_PROGRESS_EVENT)
                                tripManager.loadTrip(tripName)
                                Log.i(LOGGER_KEY, "Trip: '$tripName' is loaded")
                            } finally {
                                sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT)
                            }
                        }
                    }
                }
            }
        }
    }
}