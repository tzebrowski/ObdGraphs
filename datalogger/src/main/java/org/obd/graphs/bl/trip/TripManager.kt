package org.obd.graphs.bl.trip

import android.content.Context
import android.util.Log
import com.github.mikephil.charting.data.Entry
import org.obd.graphs.ValueScaler
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.getContext
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.isEnabled
import org.obd.graphs.profile.getSelectedProfile
import org.obd.graphs.profile.getProfiles
import org.obd.metrics.api.model.ObdMetric
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

val tripManager: TripManager = TripManager()

private const val LOGGER_TAG = "TripManager"
private const val MIN_TRIP_LENGTH = 5

private const val TRIP_DIRECTORY = "trips"

class TripManager {

    private val valueScaler = ValueScaler()

    private val dateFormat: SimpleDateFormat =
        SimpleDateFormat("MM.dd HH:mm:ss", Locale.getDefault())

    private val tripModelSerializer = TripModelSerializer()
    private val tripCache = TripCache()

    fun addTripEntry(metric: ObdMetric) {
        try {
            tripCache.getTrip { trip ->
                val ts = (System.currentTimeMillis() - trip.startTs).toFloat()
                val key = metric.command.pid.id
                val newRecord =
                    Entry(ts, valueScaler.scaleToNewRange(metric), key)

                if (trip.entries.containsKey(key)) {
                    val tripEntry = trip.entries[key]!!
                    tripEntry.metrics.add(
                        Metric(
                            entry = newRecord,
                            ts = metric.timestamp,
                            rawAnswer = metric.raw
                        )
                    )
                } else {
                    trip.entries[key] = SensorData(
                        id = key,
                        metrics = mutableListOf(
                            Metric(
                                entry = newRecord,
                                ts = metric.timestamp,
                                rawAnswer = metric.raw
                            )
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            Log.e(LOGGER_TAG, "Failed to add cache entry", e)
        }
    }

    fun getCurrentTrip(): Trip {
        if (null == tripCache.getTrip()) {
            startNewTrip(System.currentTimeMillis())
        }

        val trip = tripCache.getTrip()!!
        Log.i(LOGGER_TAG, "Get current trip ts: '${formatTimestamp(trip.startTs)}'")
        return trip
    }

    fun startNewTrip(newTs: Long) {
        Log.i(LOGGER_TAG, "Starting new trip, timestamp: '${formatTimestamp(newTs)}'")
        updateCache(newTs)
    }

    fun saveCurrentTrip() {
        tripCache.getTrip { trip ->

            val histogram = dataLogger.diagnostics().histogram()
            val pidDefinitionRegistry = dataLogger.pidDefinitionRegistry()

            trip.entries.forEach { (t, u) ->
                val p = pidDefinitionRegistry.findBy(t)
                p?.let {
                    val histogramSupplier = histogram.findBy(it)
                    u.max = histogramSupplier.max
                    u.min = histogramSupplier.min
                    u.mean = histogramSupplier.mean
                }
            }

            val recordShortTrip = Prefs.isEnabled("pref.trips.recordings.save.short.trip")

            val tripLength = getTripLength(trip)

            Log.i(LOGGER_TAG, "Recorded trip, length: ${tripLength}s")

            if (recordShortTrip || tripLength > MIN_TRIP_LENGTH) {
                val tripStartTs = trip.startTs

                val filter = "trip-${getSelectedProfile()}-${tripStartTs}"
                val alreadySaved = findAllTripsBy(filter)

                if (alreadySaved.isNotEmpty()) {
                    Log.e(
                        LOGGER_TAG,
                        "It seems that Trip which start same date='${filter}' is already saved."
                    )
                } else {
                    try {
                        val content: String =
                            tripModelSerializer.serializer.writeValueAsString(trip)

                        val fileName =
                            "trip-${getSelectedProfile()}-${tripStartTs}-${tripLength}.json"
                        Log.i(
                            LOGGER_TAG,
                            "Saving the trip to the file: '$fileName'. Length: ${tripLength}s"
                        )
                        writeFile(getContext()!!, fileName, content)
                        Log.i(
                            LOGGER_TAG,
                            "Trip was written to the file: '$fileName'. Length: ${tripLength}s"
                        )
                    }catch (e: java.lang.Exception) {
                        Log.e(LOGGER_TAG,"Failed to save trip", e)
                    }
                }
            } else {
                Log.w(LOGGER_TAG, "Trip was not saved. Trip time is less than ${MIN_TRIP_LENGTH}s")
            }
        }
    }

    fun findAllTripsBy(filter: String = ""): MutableCollection<TripFileDesc> {
        Log.i(LOGGER_TAG, "Find all trips by filter: '$filter'")

        val profiles = getProfiles()
        val files = File(getTripsDirectory(getContext()!!)).list()
        if (files == null) {
            Log.i(LOGGER_TAG, "Find all trips by filter: '${filter}'. Result size: 0")
            return mutableListOf()
        } else {
            val result = files
                .filter { if (filter.isNotEmpty()) it.startsWith(filter) else true }
                .filter { it.startsWith("trip_") || it.contains("") }
                .filter { it.substring(0, it.length - 5).split("-").size > 3 }
                .filter { it.contains(getSelectedProfile()) }
                .mapNotNull { fileName ->
                    val p = fileName.substring(0, fileName.length - 5).split("-")
                    val profileId = p[1]
                    val profileLabel = profiles[profileId]!!

                    TripFileDesc(
                        fileName = fileName,
                        profileId = profileId,
                        profileLabel = profileLabel,
                        startTime = p[2],
                        tripTimeSec = p[3]
                    )
                }
                .sortedByDescending { it.startTime.toLongOrNull() }
                .toMutableList()
            Log.i(LOGGER_TAG, "Find all trips by filter: '${filter}'. Result size: ${result.size}")
            return result
        }
    }

    fun deleteTrip(trip: TripFileDesc) {
        Log.i(LOGGER_TAG, "Deleting '${trip.fileName}' from the storage.")
        val file = File(getTripsDirectory(getContext()!!), trip.fileName)
        file.delete()
        Log.i(LOGGER_TAG, "Trip '${trip.fileName}' has been deleted from the storage.")
    }

    fun loadTrip(tripName: String) {
        Log.i(LOGGER_TAG, "Loading '$tripName' from disk.")

        if (tripName.isEmpty()) {
            updateCache(System.currentTimeMillis())
        } else {
            val file = File(getTripsDirectory(getContext()!!), tripName)
            try {
                val trip: Trip = tripModelSerializer.deserializer.readValue(file, Trip::class.java)
                Log.i(LOGGER_TAG, "Trip '${file.absolutePath}' was loaded from the disk.")
                tripCache.updateTrip(trip)
            } catch (e: FileNotFoundException) {
                Log.e(LOGGER_TAG, "Did not find trip '$tripName'.", e)
                updateCache(System.currentTimeMillis())
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
            val file = getTripFile(context, fileName)
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

    private fun getTripFile(context: Context, fileName: String): File =
        File(getTripsDirectory(context), fileName)

    private fun getTripsDirectory(context: Context) =
        "${context.getExternalFilesDir(TRIP_DIRECTORY)?.absolutePath}"

    private fun updateCache(newTs: Long) {
        val trip = Trip(startTs = newTs, entries = mutableMapOf())
        tripCache.updateTrip(trip)
        Log.i(LOGGER_TAG, "Init new Trip with timestamp: '${formatTimestamp(newTs)}'")
    }

    private fun getTripLength(trip: Trip): Long = if (trip.startTs == 0L) 0 else {
        (Date().time - trip.startTs) / 1000
    }

    private fun formatTimestamp(ts: Long) =
        dateFormat.format(Date(ts))
}