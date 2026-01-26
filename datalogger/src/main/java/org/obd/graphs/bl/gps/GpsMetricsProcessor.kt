package org.obd.graphs.bl.gps

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import org.obd.graphs.bl.datalogger.MetricsProcessor
import org.obd.graphs.getContext
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.Reply
import org.obd.metrics.api.model.ReplyObserver
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.ValueType
import org.obd.metrics.transport.message.ConnectorResponse

private const val TAG = "GpsMetricsProcessor"

val gpsMetricsProcessor: MetricsProcessor =  GpsMetricsProcessor()

internal class GpsMetricsProcessor : MetricsProcessor {

    private val raw = object : ConnectorResponse {
        override fun at(p0: Int): Byte = "".toByte()
        override fun capacity(): Long = 0
        override fun remaining(): Int = 0
    }

    private var currentLocation: Location? = null

    private var replyObserver: ReplyObserver<Reply<*>>? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val latPid = ObdCommand(PidDefinition(9977771L, 2,"","", "Lat", "deg", "GPS Latitude", -90, 90, ValueType.DOUBLE))
    private val lonPid = ObdCommand(PidDefinition(9977772L, 2,"","", "Lon", "deg", "GPS Longitude", -180, 180, ValueType.DOUBLE))
    private val altPid = ObdCommand(PidDefinition(9977773L, 2,"","", "Alt", "m", "GPS Altitude", -100, 10000, ValueType.DOUBLE))

    override fun init(replyObserver: ReplyObserver<Reply<*>>) {
        this.replyObserver = replyObserver

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext()!!)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    currentLocation = location
                    if (Log.isLoggable(TAG, Log.INFO)) {
                        Log.i(TAG, "GPS Update: ${location.latitude}, ${location.longitude}")
                    }
                    emitMetric(latPid, location.latitude)
                    emitMetric(lonPid, location.longitude)
                    emitMetric(altPid, location.altitude)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRunning(vehicleCapabilities: org.obd.metrics.api.model.VehicleCapabilities?) {
        Log.i(TAG, "Starting GPS updates")
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500)
                .setMinUpdateIntervalMillis(500)
                .build()

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start GPS", e)
        }
    }

    override fun onStopped() {
        Log.i(TAG, "Stopping GPS updates")
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun postValue(obdMetric: ObdMetric) {

        val loc = currentLocation ?: return
        if (obdMetric.command.pid.id in 9977771L..9977773L) {
            return
        }

        emitMetric(latPid, loc.latitude)
        emitMetric(lonPid, loc.longitude)
        emitMetric(altPid, loc.altitude)
    }

    private fun emitMetric(command: ObdCommand, value: Number) {
        Log.e(TAG, "Emitting: ${command.pid.pid}, $value")
        val metric = ObdMetric.builder()
            .command(command)
            .value(value)
            .raw(raw)
            .build()

        replyObserver?.onNext(metric)
    }
}