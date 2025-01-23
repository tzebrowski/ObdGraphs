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
import org.obd.graphs.cacheManager

private const val CACHE_TRIP_PROPERTY_NAME = "cache.trip.current"

internal class TripCache {

    init {
        val trip = Trip(startTs = System.currentTimeMillis(), entries = mutableMapOf())
        updateTrip(trip)
        Log.i("tripCache", "Init Trip with stamp: ${trip.startTs}")
    }

    fun getTrip(handler: (t: Trip) -> Unit) {
        getTrip()?.let(handler)
    }

    fun getTrip(): Trip? = cacheManager.findEntry(CACHE_TRIP_PROPERTY_NAME) as Trip?

    fun updateTrip(t: Trip) {
        cacheManager.updateEntry(CACHE_TRIP_PROPERTY_NAME, t)
    }
}