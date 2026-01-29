package org.obd.graphs.bl.gps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.HandlerThread
import android.util.Log
import com.google.android.gms.location.*
import org.obd.graphs.Permissions
import org.obd.graphs.bl.datalogger.MetricsProcessor
import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.getContext
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.Reply
import org.obd.metrics.api.model.ReplyObserver
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.transport.message.ConnectorResponse

private const val TAG = "GpsMetricsEmitter"

private const val UPDATE_INTERVAL_MS = 1000L
private const val FASTEST_INTERVAL_MS = 500L

val gpsMetricsEmitter: MetricsProcessor = GpsMetricsEmitter()

internal class GpsMetricsEmitter : MetricsProcessor {

    private val raw = object : ConnectorResponse {
        override fun at(p0: Int): Byte = "".toByte()
        override fun capacity(): Long = 0
        override fun remaining(): Int = 0
    }

    private var replyObserver: ReplyObserver<Reply<*>>? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    private var handlerThread: HandlerThread? = null

    private lateinit var latitudeCommand: ObdCommand
    private lateinit var longitudeCommand: ObdCommand
    private lateinit var altitudeCommand: ObdCommand

    override fun init(replyObserver: ReplyObserver<Reply<*>>) {
        this.replyObserver = replyObserver
        val registry = dataLogger.getPidDefinitionRegistry()
        latitudeCommand = ObdCommand(registry.findBy(Pid.GPS_LAT_PID_ID.id))
        longitudeCommand = ObdCommand(registry.findBy(Pid.GPS_LON_PID_ID.id))
        altitudeCommand = ObdCommand(registry.findBy(Pid.GPS_ALT_PID_ID.id))
    }

    @SuppressLint("MissingPermission")
    override fun onRunning(vehicleCapabilities: org.obd.metrics.api.model.VehicleCapabilities?) {
        val context = getContext() ?: return

        if (!dataLoggerSettings.instance().adapter.gpsCollecetingEnabled) {
            Log.i(TAG, "GPS collection disabled in settings.")
            return
        }

        if (!Permissions.hasLocationPermissions(context)) {
            Log.w(TAG, "Missing Location Permissions.")
            return
        }

        if (!Permissions.isLocationEnabled(context)) {
            Log.w(TAG, "System Location is disabled.")
            return
        }

        startLocationUpdates(context)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(context: Context) {
        try {
            Log.i(TAG, "Starting GPS updates (Target: ${UPDATE_INTERVAL_MS}ms)")

            if (fusedLocationClient == null) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            }

            // Init Background Thread
            if (handlerThread == null) {
                handlerThread = HandlerThread("GpsLocationThread").apply { start() }
            }

            if (locationCallback == null) {
                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        for (location in locationResult.locations) {

                            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                                Log.v(TAG, "Fix: ${location.latitude},${location.longitude} Acc:${location.accuracy}m Prov:${location.provider}")
                            }

                            processLocation(location)
                        }
                    }
                }
            }

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
                .setWaitForAccurateLocation(false)
                .build()

            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                handlerThread!!.looper
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start GPS updates", e)
        }
    }

    override fun onStopped() {
        Log.i(TAG, "Stopping GPS updates")
        try {
            fusedLocationClient?.removeLocationUpdates(locationCallback!!)
            handlerThread?.quitSafely()
            handlerThread = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping GPS updates", e)
        }
    }

    override fun postValue(obdMetric: ObdMetric) { }

    private fun processLocation(location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        val alt = location.altitude

        if (lat == 0.0 && lon == 0.0) return

        if (::latitudeCommand.isInitialized) emitMetric(latitudeCommand, lat)
        if (::longitudeCommand.isInitialized) emitMetric(longitudeCommand, lon)
        if (::altitudeCommand.isInitialized) emitMetric(altitudeCommand, alt)
    }



    private fun emitMetric(command: ObdCommand, value: Number) {
        replyObserver?.onNext(ObdMetric.builder()
            .command(command)
            .value(value)
            .raw(raw)
            .build())
    }
}