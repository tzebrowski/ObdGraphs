package org.obd.graphs.bl.trip

import org.obd.graphs.cacheManager

private const val CACHE_TRIP_PROPERTY_NAME = "cache.trip.current"

class TripCache {

    fun getTrip(handler: (t: Trip) -> Unit) {
        val t = cacheManager.findEntry(CACHE_TRIP_PROPERTY_NAME) as Trip?
        t?.let(handler)
    }

    fun getTrip(): Trip? = cacheManager.findEntry(CACHE_TRIP_PROPERTY_NAME) as Trip?

    fun updateTrip(t: Trip) {
        cacheManager.updateEntry(CACHE_TRIP_PROPERTY_NAME, t)
    }
}