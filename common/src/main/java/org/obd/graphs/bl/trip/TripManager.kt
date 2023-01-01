package org.obd.graphs.bl.trip

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
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
import org.obd.metrics.transport.message.ConnectorResponse
import org.obd.metrics.transport.message.ConnectorResponseFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private const val CACHE_TRIP_PROPERTY_NAME = "cache.trip.current"
private const val LOGGER_TAG = "TripRecorder"
private const val MIN_TRIP_LENGTH = 5
private val EMPTY_CONNECTOR_RESPONSE = ConnectorResponseFactory.wrap(byteArrayOf())

private class ConnectorResponseSerializer() :
    StdSerializer<ConnectorResponse>(ConnectorResponse::class.java) {

    @Throws(IOException::class)
    override fun serialize(
        value: ConnectorResponse,
        gen: JsonGenerator,
        provider: SerializerProvider
    ) {
        gen.writeString(value.message)
    }
}

private class NopeConnectorResponseSerializer() :
    StdSerializer<ConnectorResponse>(ConnectorResponse::class.java) {

    @Throws(IOException::class)
    override fun serialize(
        value: ConnectorResponse,
        gen: JsonGenerator,
        provider: SerializerProvider
    ) {
        gen.writeString("")
    }
}

private class ConnectorResponseDeserializer() :
    StdDeserializer<ConnectorResponse>(String::class.java) {

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): ConnectorResponse {
        return EMPTY_CONNECTOR_RESPONSE
    }
}

data class TripFileDesc(
    val fileName: String,
    val profileId: String,
    val profileLabel: String,
    val startTime: String,
    val tripTimeSec: String
)

data class Metric(
    val entry: Entry,
    val ts: Long,
    val rawAnswer: ConnectorResponse
)

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
            Log.i(LOGGER_TAG, "Init Trip with stamp: ${trip.startTs}")
        }
    }

    private val valueScaler = ValueScaler()
    private val context: Context by lazy { getContext()!! }
    private val dateFormat: SimpleDateFormat =
        SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault())

    fun addTripEntry(metric: ObdMetric) {
        try {
            getTripFromCache()?.let { trip ->
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
        if (null == getTripFromCache()) {
            startNewTrip(System.currentTimeMillis())
        }

        val trip = getTripFromCache()!!
        Log.i(LOGGER_TAG, "Get current trip ts: '${dateFormat.format(Date(trip.startTs))}'")
        return trip
    }

    fun startNewTrip(newTs: Long) {
        Log.i(LOGGER_TAG, "Starting new trip, timestamp: '${dateFormat.format(Date(newTs))}'")
        updateCache(newTs)
    }

    fun saveCurrentTrip() {
        getTripFromCache()?.let { trip ->

            val histogram = DataLogger.instance.diagnostics().histogram()
            val pidDefinitionRegistry = DataLogger.instance.pidDefinitionRegistry()

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
                val startString = dateFormat.format(Date(trip.startTs))

                val filter = "trip-${getCurrentProfile()}-${startString}"
                val alreadySaved = findAllTripsBy(filter)

                if (alreadySaved.isNotEmpty()) {
                    Log.e(
                        LOGGER_TAG,
                        "It seems that Trip which start same date='${filter}' is already saved."
                    )
                } else {
                    val content: String = objectMapper().writeValueAsString(trip)

                    val fileName = "trip-${getCurrentProfile()}-${startString}-${tripLength}.json"
                    Log.i(
                        LOGGER_TAG,
                        "Saving the trip to the file: '$fileName'. Length: ${tripLength}s"
                    )
                    writeFile(context, fileName, content)
                    Log.i(
                        LOGGER_TAG,
                        "Trip was written to the file: '$fileName'. Length: ${tripLength}s"
                    )
                }
            } else {
                Log.i(LOGGER_TAG, "Trip was no saved. Trip time is less than ${tripLength}s")
            }
        }
    }

    fun findAllTripsBy(filter: String = ""): MutableCollection<TripFileDesc> {
        Log.i(LOGGER_TAG, "Find all trips with filter: '$filter'")

        val profiles = getProfileList()
        val files = File(getTripsDirectory(context)).list()
        if (files == null) {
            Log.i(LOGGER_TAG, "Find all trips by filter: '${filter}'. Result size: 0")
            return mutableListOf()
        } else {
            val result = files
                .filter { if (filter.isNotEmpty()) it.startsWith(filter) else true }
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
            Log.i(LOGGER_TAG, "Find all trips by filter: '${filter}'. Result size: ${result.size}")
            return result
        }
    }

    fun deleteTrip(trip: TripFileDesc) {
        Log.i(LOGGER_TAG, "Deleting '${trip.fileName}' from the storage.")
        val file = File(getTripsDirectory(context), trip.fileName)
        file.delete()
        Log.i(LOGGER_TAG, "Trip '${trip.fileName}' has been deleted from the storage.")
    }

    fun loadTrip(tripName: String) {
        Log.i(LOGGER_TAG, "Loading '$tripName' from disk.")

        if (tripName.isEmpty()) {
            updateCache(System.currentTimeMillis())
        } else {
            val file = File(getTripsDirectory(context), tripName)
            try {

                val jackson: ObjectMapper by lazy {
                    jacksonObjectMapper().apply {
                        val module = SimpleModule()
                        module.addSerializer(
                            ConnectorResponse::class.java,
                            ConnectorResponseSerializer()
                        )
                        module.addDeserializer(
                            ConnectorResponse::class.java,
                            ConnectorResponseDeserializer()
                        )
                        registerModule(module)
                    }
                }

                val trip: Trip = jackson.readValue(file, Trip::class.java)
                Log.i(LOGGER_TAG, "Trip '${file.absolutePath}' was loaded from the disk.")
                Cache[CACHE_TRIP_PROPERTY_NAME] = trip
            } catch (e: FileNotFoundException) {
                Log.e(LOGGER_TAG, "Did not find trip '$tripName'.", e)
                updateCache(System.currentTimeMillis())
            }
        }
    }

    private fun objectMapper(): ObjectMapper {
        val jackson: ObjectMapper by lazy {
            jacksonObjectMapper().apply {
                val module = SimpleModule()
                val serializeConnectorResponse =
                    Prefs.getBoolean("pref.debug.trip.save.connector_response", false)
                if (serializeConnectorResponse) {
                    module.addSerializer(
                        ConnectorResponse::class.java,
                        ConnectorResponseSerializer()
                    )
                } else {
                    module.addSerializer(
                        ConnectorResponse::class.java,
                        NopeConnectorResponseSerializer()
                    )
                }

                module.addDeserializer(
                    ConnectorResponse::class.java,
                    ConnectorResponseDeserializer()
                )
                registerModule(module)
            }
        }
        return jackson
    }

    private fun writeFile(
        context: Context,
        fileName: String,
        content: String
    ) {
        var fd: FileOutputStream? = null
        try {
            val file = getTripFileRef(context, fileName)
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

    private fun getTripFileRef(context: Context, fileName: String): File =
        File(getTripsDirectory(context), fileName)


    private fun getTripsDirectory(context: Context) =
        "${context.getExternalFilesDir("trips")?.absolutePath}"

    private fun updateCache(newTs: Long) {
        val trip = Trip(startTs = newTs, entries = mutableMapOf())
        Cache[CACHE_TRIP_PROPERTY_NAME] = trip

        Log.i(LOGGER_TAG, "Init new Trip with timestamp: '${dateFormat.format(Date(newTs))}'")
    }

    private fun getTripFromCache(): Trip? = Cache[CACHE_TRIP_PROPERTY_NAME] as Trip?

    private fun getTripLength(trip: Trip): Long = if (trip.startTs == 0L) 0 else {
        (Date().time - trip.startTs) / 1000
    }
}