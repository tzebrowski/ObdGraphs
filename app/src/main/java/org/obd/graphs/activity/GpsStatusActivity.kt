 /**
 * Copyright 2019-2026, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.activity

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.obd.graphs.Permissions
import org.obd.graphs.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GpsStatusActivity : AppCompatActivity() {
    private lateinit var txtProvider: TextView
    private lateinit var txtAccuracy: TextView
    private lateinit var txtLat: TextView
    private lateinit var txtLon: TextView
    private lateinit var txtSatellites: TextView
    private lateinit var txtLogs: TextView

    private var locationManager: LocationManager? = null
    private var gnssCallback: GnssStatus.Callback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gps_status)

        txtProvider = findViewById(R.id.txt_provider)
        txtAccuracy = findViewById(R.id.txt_accuracy)
        txtLat = findViewById(R.id.txt_lat)
        txtLon = findViewById(R.id.txt_lon)
        txtSatellites = findViewById(R.id.txt_satellites)
        txtLogs = findViewById(R.id.txt_logs)

        if (!Permissions.hasLocationPermissions(this)) {
            log("ERROR: Missing permissions. Please grant Precise Location.")
            Permissions.requestLocationPermissions(this)
        } else {
            if (!Permissions.hasLocationPermissions(this)) {
                Permissions.requestLocationPermissions(this)
            }

            startLocationUpdates()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Permissions.hasLocationPermissions(this)) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // 1. Register for Location Updates (GPS)
            // MIN_DISTANCE = 0.0f ensures we get updates even if standing still (critical for debugging)
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0.0f,
                locationListener,
                Looper.getMainLooper(),
            )
            log("Started requesting GPS_PROVIDER updates...")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                gnssCallback =
                    object : GnssStatus.Callback() {
                        override fun onSatelliteStatusChanged(status: GnssStatus) {
                            val count = status.satelliteCount
                            var used = 0
                            for (i in 0 until count) {
                                if (status.usedInFix(i)) used++
                            }
                            updateSatellites(count, used)
                        }
                    }
                locationManager?.registerGnssStatusCallback(gnssCallback!!, Handler(Looper.getMainLooper()))
                log("Registered GNSS Status Callback.")
            }
        } catch (e: Exception) {
            log("EXCEPTION: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        locationManager?.removeUpdates(locationListener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssCallback != null) {
            locationManager?.unregisterGnssStatusCallback(gnssCallback!!)
        }
        log("Updates stopped.")
    }

    private val locationListener =
        object : LocationListener {
            override fun onLocationChanged(location: Location) {
                updateUI(location)
            }

            override fun onProviderEnabled(provider: String) {
                log("Provider Enabled: $provider")
            }

            override fun onProviderDisabled(provider: String) {
                log("Provider Disabled: $provider")
                txtProvider.text = "GPS DISABLED"
            }
        }

    private fun updateUI(location: Location) {
        // Provider
        txtProvider.text = location.provider?.uppercase()

        // Accuracy - Color code it
        val acc = location.accuracy
        txtAccuracy.text = "±${String.format("%.1f", acc)}m"

        if (acc > 500) {
            txtAccuracy.setTextColor(0xFFFF0000.toInt()) // Red - Likely Coarse Location
            log("WARNING: High inaccuracy ($acc m). Coarse permission active?")
        } else if (acc < 20) {
            txtAccuracy.setTextColor(0xFF00FF00.toInt()) // Green - Good Fix
        } else {
            txtAccuracy.setTextColor(0xFFFFFFFF.toInt()) // White - Average
        }

        // Coordinates
        txtLat.text = String.format("%.6f", location.latitude)
        txtLon.text = String.format("%.6f", location.longitude)

        log("Fix: ${location.provider} | Acc: ${location.accuracy}m")
    }

    private fun updateSatellites(
        total: Int,
        used: Int,
    ) {
        txtSatellites.text = "$total Visible / $used Used"
    }

    private fun log(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val currentText = txtLogs.text.toString()
        val newText = "[$time] $msg\n$currentText" // Prepend
        if (newText.length > 5000) {
            txtLogs.text = newText.substring(0, 5000)
        } else {
            txtLogs.text = newText
        }
    }
}
