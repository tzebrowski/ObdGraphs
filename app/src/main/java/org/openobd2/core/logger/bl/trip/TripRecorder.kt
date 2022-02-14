package org.openobd2.core.logger.bl.trip

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.mikephil.charting.data.Entry
import org.obd.metrics.ObdMetric
import org.openobd2.core.logger.ApplicationContext
import org.openobd2.core.logger.Cache
import org.openobd2.core.logger.ui.graph.Scaler
import java.io.File
import java.io.FileOutputStream
import java.util.*

private const val CACHE_ENTRIES_PROPERTY_NAME = "cache.graph.entries"
private const val CACHE_TS_PROPERTY_NAME = "cache.graph.start_timestamp"
private const val LOGGER_KEY = "TripRecorder"

@JsonIgnoreProperties(ignoreUnknown = true)
data class Trip(val firstTimeStamp: Long, val entries: MutableMap<String, MutableList<Entry>>) {}

class TripRecorder private constructor() {

    companion object {
        @JvmStatic
        val INSTANCE:TripRecorder = TripRecorder().apply {
            if (Cache[CACHE_ENTRIES_PROPERTY_NAME] == null) {
                Cache[CACHE_ENTRIES_PROPERTY_NAME] = mutableMapOf<String, MutableList<Entry>>()
            }
            if (firstTimeStamp == null) {
                firstTimeStamp = System.currentTimeMillis().apply {
                    Cache[CACHE_TS_PROPERTY_NAME] = this
                }
                Log.i(LOGGER_KEY, "Init cache with stamp: $firstTimeStamp")
            }
        }
    }

    private var firstTimeStamp: Long? = null
    private val scaler  = Scaler()
    private val context: Context by lazy { ApplicationContext }

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
        Log.e(LOGGER_KEY,"Get current trip ts: $firstTimeStamp")
        return Trip(firstTimeStamp, cacheEntries)
    }

    fun startNewTrip(firstTimeStamp: Long,ts: Float) {
        Log.e(LOGGER_KEY, "Starting new trip $ts, date: $firstTimeStamp")
        resetCache(ts, firstTimeStamp)
    }

    fun saveTrip(context: Context, ts: Float) {

        val trip = getCurrentTrip()
        val content: String = jacksonObjectMapper().writeValueAsString(trip)
        val fileName = "trip_${Date()}.json"

        Log.i(LOGGER_KEY, "Saving the trip to the file: $fileName")

        writeFile(context, fileName, content)

        Log.i(LOGGER_KEY, "Trip was written to the file: $fileName")
    }

    fun findAllTripsBy(query:String = ""): MutableList<String> {
        return context.cacheDir.list().filter { it.startsWith("trip_") || it.contains("") }
            .sortedByDescending { it }
            .toMutableList()
    }


    fun loadTrip(tripName: String): Trip {
        val file = File(context.cacheDir, tripName)
        val trip:Trip = jacksonObjectMapper().readValue<Trip>(file, Trip::class.java)
        Log.i(LOGGER_KEY,"Trip '$tripName' was loaded from the cache")

        trip.run{
            Cache[CACHE_TS_PROPERTY_NAME] = firstTimeStamp
            Cache[CACHE_ENTRIES_PROPERTY_NAME] = entries
        }

        return trip
    }

    private fun writeFile(
        context: Context,
        fileName: String,
        content: String
    ) {
        val file = File(context.cacheDir, fileName)
        val fd = FileOutputStream(file)
        fd.write(content.toByteArray())
        fd.flush()
        fd.close()
    }

    private fun resetCache(ts: Float, firstTimeStamp: Long) {
        Cache[CACHE_ENTRIES_PROPERTY_NAME] = mutableMapOf<String, MutableList<Entry>>()
        Cache[CACHE_TS_PROPERTY_NAME] = firstTimeStamp
    }
}