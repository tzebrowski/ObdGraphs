package org.openobd2.core.logger.bl.trip

import android.util.Log
import com.github.mikephil.charting.data.Entry
import org.obd.metrics.ObdMetric
import org.openobd2.core.logger.ui.common.Cache
import org.openobd2.core.logger.ui.graph.Scaler
import java.util.*

private const val CACHE_ENTRIES_PROPERTY_NAME = "cache.graph.entries"
private const val CACHE_TS_PROPERTY_NAME = "cache.graph.ts"
private const val CACHE_X_AXIS_MIN_PROPERTY_NAME = "cache.graph.x_axis.min"
private const val LOGGER_KEY = "TripRecorder"

data class Trip(val firstTimeStamp: Long, val entries: MutableMap<String, MutableList<Entry>>, val ts: Float) {}

class TripRecorder private constructor() {

    companion object {
        @JvmStatic
        val INSTANCE:TripRecorder = TripRecorder().apply {
            if (Cache[CACHE_ENTRIES_PROPERTY_NAME] == null) {
                Cache[CACHE_ENTRIES_PROPERTY_NAME] = mutableMapOf<String, MutableList<Entry>>()
            }

            Cache[CACHE_X_AXIS_MIN_PROPERTY_NAME]  = 0f

            if (firstTimeStamp == null) {
                firstTimeStamp = System.currentTimeMillis().apply {
                    Cache[CACHE_TS_PROPERTY_NAME] = this
                }
                Log.i(LOGGER_KEY, "Init cache stamp: $firstTimeStamp")
            }
        }
    }

    private var firstTimeStamp: Long? = null
    private val scaler  = Scaler()

    fun addTripEntry(reply: ObdMetric) {
        try {
            Cache[CACHE_ENTRIES_PROPERTY_NAME]?.let {
                val cache = it as MutableMap<String, MutableList<Entry>>
                val timestamp = (System.currentTimeMillis() - firstTimeStamp!!).toFloat()
                val entry = Entry(timestamp, scaler.scaleToNewRange(reply))
                cache.getOrPut(reply.command.pid.description) {
                    mutableListOf<Entry>()
                }.add(entry)
            }
        }catch (e: Throwable){
            Log.e(LOGGER_KEY,"Failed to add cache entry",e)
        }
    }

    fun getCurrentTrip(): Trip {
        val firstTimeStamp = Cache[CACHE_TS_PROPERTY_NAME] as Long
        val cacheEntries = Cache[CACHE_ENTRIES_PROPERTY_NAME] as MutableMap<String, MutableList<Entry>>
        val ts = Cache[CACHE_X_AXIS_MIN_PROPERTY_NAME] as Float
        return Trip(firstTimeStamp, cacheEntries, ts)
    }
    fun startNewTrip(ts: Float){
        if (ts > 0) {
            Log.i(LOGGER_KEY, "Starting new trip $ts, date: ${Date()}")
            Cache[CACHE_X_AXIS_MIN_PROPERTY_NAME] = ts
        }
    }

    fun getAllTrips(){

    }

    fun deleteTrip(){

    }
}