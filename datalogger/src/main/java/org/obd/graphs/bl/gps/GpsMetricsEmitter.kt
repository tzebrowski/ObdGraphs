package org.obd.graphs.bl.gps

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.HandlerThread
import android.util.Log
import androidx.core.content.ContextCompat
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
private const val MIN_TIME_MS = 1000L
private const val MIN_DISTANCE_M = 0.1f

val gpsMetricsEmitter: MetricsProcessor = GpsMetricsEmitter()

internal class GpsMetricsEmitter : MetricsProcessor {

    private val raw = object : ConnectorResponse {
        override fun at(p0: Int): Byte = "".toByte()
        override fun capacity(): Long = 0
        override fun remaining(): Int = 0
    }

    private var replyObserver: ReplyObserver<Reply<*>>? = null
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
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

        if (!dataLoggerSettings.instance().adapter.gpsCollecetingEnabled) return
        if (!Permissions.hasLocationPermissions(context)) return

        try {
            Log.i(TAG, "Starting Raw GPS Provider (Bypassing Fused)")

            if (locationManager == null) {
                locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            }

            if (handlerThread == null) {
                handlerThread = HandlerThread("RawGpsThread").apply { start() }
            }

            val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            Log.i(TAG, "GPS Permissions Status -> Fine: $hasFine, Coarse: $hasCoarse")

            if (hasCoarse && !hasFine) {
                Log.w(TAG, "WARNING: User granted only APPROXIMATE location. GPS data will be snapped to a grid.")
            }

            if (locationListener == null) {
                locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "Fix: ${location.latitude},${location.longitude} Prov:${location.provider}")
                        }
                        processLocation(location)
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
            }

            val provider = if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LocationManager.GPS_PROVIDER
            } else {
                LocationManager.NETWORK_PROVIDER
            }

            Log.i(TAG, "We will use following provider='${provider}'")

            locationManager?.requestLocationUpdates(
                provider,
                MIN_TIME_MS,
                MIN_DISTANCE_M,
                locationListener!!,
                handlerThread!!.looper
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Raw GPS updates", e)
        }
    }

    private fun processLocation(location: Location) {
        if (location.latitude == 0.0 && location.longitude == 0.0) return

        if (::latitudeCommand.isInitialized) emitMetric(latitudeCommand, location.latitude)
        if (::longitudeCommand.isInitialized) emitMetric(longitudeCommand, location.longitude)
        if (::altitudeCommand.isInitialized) emitMetric(altitudeCommand, location.altitude)
    }

    override fun onStopped() {
        Log.i(TAG, "Stopping GPS updates")
        try {
            locationManager?.removeUpdates(locationListener!!)
            handlerThread?.quitSafely()
            handlerThread = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping GPS updates", e)
        }
    }

    override fun postValue(obdMetric: ObdMetric) { }

    private fun emitMetric(command: ObdCommand, value: Number) {
        replyObserver?.onNext(ObdMetric.builder()
            .command(command)
            .value(value)
            .raw(raw)
            .build())
    }
}