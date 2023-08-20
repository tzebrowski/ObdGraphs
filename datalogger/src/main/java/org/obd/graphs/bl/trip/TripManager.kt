package org.obd.graphs.bl.trip

import org.obd.metrics.api.model.ObdMetric

interface TripManager {
    fun addTripEntry(metric: ObdMetric)
    fun getCurrentTrip(): Trip
    fun startNewTrip(newTs: Long)
    fun saveCurrentTrip(f: () -> Unit)
    fun findAllTripsBy(filter: String = ""): MutableCollection<TripFileDesc>
    fun deleteTrip(trip: TripFileDesc)
    fun loadTrip(tripName: String)
}