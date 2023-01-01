package org.obd.graphs.bl.trip

import org.obd.graphs.Cache

private const val CACHE_TRIP_PROPERTY_NAME = "cache.trip.current"

class TripCache {

    fun getTrip(handler: (t: Trip) -> Unit) {
        val t = Cache[CACHE_TRIP_PROPERTY_NAME] as Trip?
        t?.let(handler)
    }

    fun getTrip(): Trip? = Cache[CACHE_TRIP_PROPERTY_NAME] as Trip?

    fun updateTrip(t: Trip) {
        Cache[CACHE_TRIP_PROPERTY_NAME] = t
    }
}