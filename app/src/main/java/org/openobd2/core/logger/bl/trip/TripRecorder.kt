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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


private const val CACHE_ENTRIES_PROPERTY_NAME = "cache.graph.entries"
private const val CACHE_TS_PROPERTY_NAME = "cache.graph.start_timestamp"
private const val LOGGER_KEY = "TripRecorder"

@JsonIgnoreProperties(ignoreUnknown = true)
data class Trip(val firstTimeStamp: Long, val entries: Map<String, MutableList<Entry>>) {}

class TripRecorder private constructor() {

    companion object {

        @JvmStatic
        val instance:TripRecorder = TripRecorder().apply {
            Cache[CACHE_ENTRIES_PROPERTY_NAME] = mutableMapOf<String, MutableList<Entry>>()
            firstTimeStamp = System.currentTimeMillis().apply {
                Cache[CACHE_TS_PROPERTY_NAME] = this
            }
            Log.i(LOGGER_KEY, "Init cache with stamp: $firstTimeStamp")
        }
    }

    private var firstTimeStamp: Long? = null
    private val scaler  = ValueScaler()
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
        Log.i(LOGGER_KEY,"Get current trip ts: '${simpleDateFormat.format(Date(firstTimeStamp))}'")
        return Trip(firstTimeStamp, cacheEntries)
    }

    fun startNewTrip(newTs: Long) {
        Log.i(LOGGER_KEY, "Starting new trip, time stamp: '${simpleDateFormat.format(Date(newTs))}'")
        updateCache(newTs)
    }

   private val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("MM.dd HH:mm:ss")

    fun saveTrip() {
        val trip = getCurrentTrip()
        val content: String = jacksonObjectMapper().writeValueAsString(trip)
        var format = simpleDateFormat
        val end = format.format(Date())
        val start = format.format(Date(trip.firstTimeStamp))

        val fileName = "trip_$start - $end.json"

        Log.i(LOGGER_KEY, "Saving the trip to the file: $fileName")

        writeFile(context, fileName, content)

        Log.i(LOGGER_KEY, "Trip was written to the file: $fileName")
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
                Cache[CACHE_TS_PROPERTY_NAME] = firstTimeStamp
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
        firstTimeStamp = newTs
    }
}