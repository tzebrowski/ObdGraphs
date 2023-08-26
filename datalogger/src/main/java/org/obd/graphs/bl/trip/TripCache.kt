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