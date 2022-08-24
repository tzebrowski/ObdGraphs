package org.obd.graphs.bl.trip

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.mikephil.charting.data.Entry
import org.obd.graphs.Cache
import org.obd.graphs.ValueScaler
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.getContext
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.isEnabled
import org.obd.graphs.profile.getCurrentProfile
import org.obd.graphs.profile.getProfileList
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.raw.RawMessage
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*


private const val CACHE_TRIP_PROPERTY_NAME = "cache.trip.current"
private const val LOGGER_KEY = "TripRecorder"
private const val MIN_TRIP_LENGTH = 5
private val EMPTY = RawMessage.wrap(byteArrayOf())

private class RawMessageToStringSerializer() : StdSerializer<RawMessage>(RawMessage::class.java) {

    @Throws(IOException::class)
    override fun serialize(value: RawMessage, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString( String(value.bytes, StandardCharsets.UTF_8))
    }
}

private class StringToRawMessageDeserializer() : StdDeserializer<RawMessage>(String::class.java) {

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): RawMessage {
        return EMPTY
    }
}

data class TripFileDesc(
    val fileName: String,
    val profileId: String,
    val profileLabel: String,
    val startTime: String,
    val tripTimeSec: String
)

data class Metric (
    val entry: Entry,
    val ts: Long,
    @JsonSerialize(using = RawMessageToStringSerializer::class)
    @JsonDeserialize(using = StringToRawMessageDeserializer::class)
    val rawAnswer: RawMessage)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SensorData(
    val id: Long,
    val metrics: MutableList<Metric>,
    var min: Number = 0,
    var max: Number = 0,
    var mean: Number = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorData

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Trip(val startTs: Long, val entries: MutableMap<Long, SensorData>)

class TripManager private constructor() {

    companion object {

        @JvmStatic
        val INSTANCE: TripManager = TripManager().apply {
            val trip = Trip(startTs = System.currentTimeMillis(), entries = mutableMapOf())
            Cache[CACHE_TRIP_PROPERTY_NAME] = trip
            Log.i(LOGGER_KEY, "Init Trip with stamp: ${trip.startTs}")
        }
    }

    private val valueScaler = ValueScaler()
    private val context: Context by lazy { getContext()!! }
    private val dateFormat: SimpleDateFormat =
        SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault())


    private val jackson:ObjectMapper  by lazy  {
        jacksonObjectMapper().apply {
            val module = SimpleModule()
            module.addSerializer(RawMessage::class.java, RawMessageToStringSerializer())
            module.addDeserializer(RawMessage::class.java, StringToRawMessageDeserializer())
            registerModule(module)
        }
    }

    fun addTripEntry(metric: ObdMetric) {
        try {
            getTripFromCache()?.let { trip ->
                val ts = (System.currentTimeMillis() - trip.startTs).toFloat()
                val key = metric.command.pid.id
                val newRecord =
                    Entry(ts, valueScaler.scaleToNewRange(metric), key)

                if (trip.entries.containsKey(key)) {
                    val tripEntry = trip.entries[key]!!
                    tripEntry.metrics.add(Metric(entry =  newRecord, ts = metric.timestamp,rawAnswer = metric.raw))
                } else {
                    trip.entries[key] = SensorData(id = key, metrics = mutableListOf(Metric(entry =  newRecord, ts = metric.timestamp,rawAnswer = metric.raw)))
                }
            }
        } catch (e: Throwable) {
            Log.e(LOGGER_KEY, "Failed to add cache entry", e)
        }
    }

    fun getCurrentTrip(): Trip {
        if (null == getTripFromCache()) {
            startNewTrip(System.currentTimeMillis())
        }

        val trip = getTripFromCache()!!
        Log.i(LOGGER_KEY, "Get current trip ts: '${dateFormat.format(Date(trip.startTs))}'")
        return trip
    }

    fun startNewTrip(newTs: Long) {
        Log.i(LOGGER_KEY, "Starting new trip, time stamp: '${dateFormat.format(Date(newTs))}'")
        updateCache(newTs)
    }


    fun saveCurrentTrip() {
        getTripFromCache()?.let { trip ->
            val histogram = DataLogger.instance.diagnostics().histogram()
            val pidDefinitionRegistry = DataLogger.instance.pidDefinitionRegistry()

            trip.entries.forEach { (t, u) ->
                val histogramSupplier = histogram.findBy(pidDefinitionRegistry.findBy(t))
                u.max = histogramSupplier.max
                u.min = histogramSupplier.min
                u.mean = histogramSupplier.mean
            }

            val endDate = Date()
            val recordShortTrip = Prefs.isEnabled("pref.trips.recordings.save.short.trip")

            val tripLength = if (trip.startTs == 0L) 0 else {
                (endDate.time - trip.startTs) / 1000
            }

            Log.i(LOGGER_KEY, "Recorded trip, length: ${tripLength}s")

            if (recordShortTrip || tripLength > MIN_TRIP_LENGTH) {
                val startString = dateFormat.format(Date(trip.startTs))

                val content: String = jackson.writeValueAsString(trip)
                val fileName = "trip-${getCurrentProfile()}-${startString}-${tripLength}.json"
                Log.i(LOGGER_KEY, "Saving the trip to the file: $fileName")
                writeFile(context, fileName, content)

                Log.i(LOGGER_KEY, "Trip was written to the file: $fileName")
            } else {
                Log.i(LOGGER_KEY, "Trip was no saved. Trip time is less than ${trip.startTs}s")
            }
        }
    }

    fun findAllTripsBy(query: String = ".json"): MutableCollection<TripFileDesc> {
        Log.i(LOGGER_KEY, "Find all trips with query: $query")

        val profiles = getProfileList()
        val files = File(getTripsDirectory(context)).list()
        if (files == null) {
            Log.i(LOGGER_KEY, "Find all trips with query: ${query}. Result size: 0")
            return mutableListOf()
        } else {
            val result = files
                .filter { it.startsWith("trip_") || it.contains("") }
                .filter { it.substring(0, it.length - 5).split("-").size > 3 }
                .filter { it.contains(getCurrentProfile()) }
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
                .sortedByDescending { dateFormat.parse(it.startTime) }
                .toMutableList()
            Log.i(LOGGER_KEY, "Find all trips with query: ${query}. Result size: ${result.size}")
            return result
        }
    }

    fun deleteTrip(trip: TripFileDesc) {
        Log.i(LOGGER_KEY, "Deleting '${trip.fileName}' from the storage.")
        val file = File(getTripsDirectory(context), trip.fileName)
        file.delete()
        Log.i(LOGGER_KEY, "Trip '${trip.fileName}' has been deleted from the storage.")
    }

    fun loadTrip(tripName: String) {
        Log.i(LOGGER_KEY, "Loading '$tripName' from disk.")

        if (tripName.isEmpty()) {
            updateCache(System.currentTimeMillis())
        } else {
            val file = File(getTripsDirectory(context), tripName)
            try {

                val trip: Trip = jackson.readValue(file, Trip::class.java)
                Log.i(LOGGER_KEY, "Trip '${file.absolutePath}' was loaded from the disk.")
                Cache[CACHE_TRIP_PROPERTY_NAME] = trip
            } catch (e: FileNotFoundException) {
                Log.e(LOGGER_KEY, "Did not find trip '$tripName'.", e)
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

            val directory = getTripsDirectory(context)
            val file = File(directory, fileName)
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

    private fun getTripsDirectory(context: Context) =
        "${context.getExternalFilesDir("trips")?.absolutePath}"

    private fun updateCache(newTs: Long) {
        val trip = Trip(startTs = newTs, entries = mutableMapOf())
        Cache[CACHE_TRIP_PROPERTY_NAME] = trip
        Log.i(LOGGER_KEY, "Init new Trip with stamp: $${trip.startTs}")
    }

    private fun getTripFromCache(): Trip? = Cache[CACHE_TRIP_PROPERTY_NAME] as Trip?
}