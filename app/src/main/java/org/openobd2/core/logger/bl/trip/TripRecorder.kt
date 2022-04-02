package org.openobd2.core.logger.bl.trip

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.mikephil.charting.data.Entry
import org.obd.metrics.ObdMetric
import org.openobd2.core.logger.ApplicationContext
import org.openobd2.core.logger.Cache
import org.openobd2.core.logger.ui.graph.ValueScaler
import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.isEnabled
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val CACHE_ENTRIES_PROPERTY_NAME = "cache.trip.entries"
private const val CACHE_TS_PROPERTY_NAME = "cache.trip.startT"
private const val LOGGER_KEY = "TripRecorder"

@JsonIgnoreProperties(ignoreUnknown = true)
data class Trip(val startTs: Long, val entries: Map<String, MutableList<Entry>>) {}

private val i: Int
    get() {
        val minTripLength = 5
        return minTripLength
    }

class TripRecorder private constructor() {

    companion object {

        @JvmStatic
        val instance:TripRecorder = TripRecorder().apply {
            Cache[CACHE_ENTRIES_PROPERTY_NAME] = mutableMapOf<String, MutableList<Entry>>()
            Cache[CACHE_TS_PROPERTY_NAME] =  System.currentTimeMillis()
            Log.i(LOGGER_KEY, "Init cache with stamp: $${Cache[CACHE_TS_PROPERTY_NAME]}")
        }
    }

    private val valueScaler  = ValueScaler()
    private val context: Context by lazy { ApplicationContext }

    fun addTripEntry(reply: ObdMetric) {
        try {
            Cache[CACHE_ENTRIES_PROPERTY_NAME]?.let {
                val cache = it as MutableMap<String, MutableList<Entry>>
                val timestamp = (System.currentTimeMillis() - (Cache[CACHE_TS_PROPERTY_NAME] as Long)).toFloat()
                val entry = Entry(timestamp, valueScaler.scaleToNewRange(reply), reply.command.pid.id)
                cache.getOrPut(reply.command.pid.description) {
                    mutableListOf()
                }.add(entry)
            }
        }catch (e: Throwable){
            Log.e(LOGGER_KEY,"Failed to add cache entry",e)
        }
    }

    fun getCurrentTrip(): Trip {
        val firstTimeStamp = Cache[CACHE_TS_PROPERTY_NAME] as Long
        val cacheEntries = Cache[CACHE_ENTRIES_PROPERTY_NAME] as MutableMap<String, MutableList<Entry>>
        Log.i(LOGGER_KEY,"Get current trip ts: '${dateFormat.format(Date(firstTimeStamp))}'")
        return Trip(firstTimeStamp, cacheEntries)
    }

    fun startNewTrip(newTs: Long) {
        Log.i(LOGGER_KEY, "Starting new trip, time stamp: '${dateFormat.format(Date(newTs))}'")
        updateCache(newTs)
    }

   private val dateFormat: SimpleDateFormat = SimpleDateFormat("MM.dd HH:mm:ss")

    fun saveCurrentTrip() {
        val trip = getCurrentTrip()

        val endDate = Date()
        val recordShortTrip = Prefs.isEnabled("pref.trips.recordings.save.short.trip")

        val tripLength = if (trip.startTs == 0L)  0 else {(endDate.time - trip.startTs) / 1000}

        Log.i(LOGGER_KEY, "Trip length $tripLength")

        val minTripLength = 5
        if (recordShortTrip  || tripLength > minTripLength) {
            val startString = dateFormat.format(Date(trip.startTs))
            val endString = dateFormat.format(endDate)

            val content: String = jacksonObjectMapper().writeValueAsString(trip)
            val fileName = "trip_$startString - $endString.json"
            Log.i(LOGGER_KEY, "Saving the trip to the file: $fileName")
            writeFile(context, fileName, content)

            Log.i(LOGGER_KEY, "Trip was written to the file: $fileName")
        } else {
            Log.i(LOGGER_KEY, "Trip was no saved. Trip time is less than ${trip.startTs}s")
        }
    }

    fun findAllTripsBy(query:String = ""): MutableList<String> {
        return context.cacheDir.list().filter { it.startsWith("trip_") || it.contains("") }
            .sortedByDescending { it }
            .toMutableList()
    }

    fun setCurrentTrip(tripName: String) {
        if (tripName.isEmpty()) {
            updateCache(System.currentTimeMillis())
        }else {
            val file = File(context.cacheDir, tripName)
            val trip: Trip = jacksonObjectMapper().readValue<Trip>(file, Trip::class.java)
            Log.i(LOGGER_KEY, "Trip '$tripName' was loaded from the cache")

            trip.run {
                Cache[CACHE_TS_PROPERTY_NAME] = startTs
                Cache[CACHE_ENTRIES_PROPERTY_NAME] = entries
            }
        }
    }

    private fun writeFile(
        context: Context,
        fileName: String,
        content: String
    ) {
        var fd: FileOutputStream? = null
        try {

            val file = File(context.cacheDir, fileName)
            fd = FileOutputStream(file).apply {
                write(content.toByteArray())
            }

        } finally {
            fd?.run {
                flush()
                close()
            }
        }
    }

    private fun updateCache(newTs: Long) {
        (Cache[CACHE_ENTRIES_PROPERTY_NAME] as MutableMap<String, MutableList<Entry>>).clear()
        Cache[CACHE_ENTRIES_PROPERTY_NAME] = mutableMapOf<String, MutableList<Entry>>()
        Cache[CACHE_TS_PROPERTY_NAME] = newTs
    }
}