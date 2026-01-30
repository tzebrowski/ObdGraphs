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
package org.obd.graphs

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import org.obd.graphs.commons.R
import pub.devrel.easypermissions.EasyPermissions

private const val TAG = "Permissions"
private const val LOCATION_REQUEST_CODE = 1001
private const val BLUETOOTH_REQUEST_CODE = 1002
private const val NOTIFICATION_REQUEST_CODE = 1003

object Permissions {
    /**
     * Returns TRUE if all required location permissions are granted.
     * Also performs a diagnostic check to warn if the user has selected "Approximate" location.
     */
    fun hasLocationPermissions(context: Context): Boolean {
        // Diagnostic Check: Detect if user selected "Approximate" (Coarse) but denied "Precise" (Fine)
        val finePermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        val coarsePermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )

        if (coarsePermission == PackageManager.PERMISSION_GRANTED &&
            finePermission != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "WARNING: User granted only APPROXIMATE location. GPS data will be snapped to a grid.")
        }

        // Standard Strict Check: Returns true only if BOTH permissions are granted
        return EasyPermissions.hasPermissions(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }

    /**
     * returns TRUE if Notification permissions are granted (Android 13+)
     */
    fun hasNotificationPermissions(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            EasyPermissions.hasPermissions(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return false

        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    fun requestNotificationPermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val perm = Manifest.permission.POST_NOTIFICATIONS
        if (EasyPermissions.hasPermissions(activity, perm)) {
            Log.v(TAG, "Notification permissions already granted.")
            return
        }

        Log.i(TAG, "Requesting missing notification permissions.")
        EasyPermissions.requestPermissions(
            activity,
            "Notification permission is required to run the data logger in the foreground.", // Replace with R.string if available
            NOTIFICATION_REQUEST_CODE,
            perm,
        )
    }

    fun requestLocationPermissions(activity: Activity) {
        val perms =
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )

        if (EasyPermissions.hasPermissions(activity, *perms)) {
            Log.v(TAG, "Location permissions already granted.")
            return
        }

        Log.i(TAG, "Requesting missing location permissions.")
        EasyPermissions.requestPermissions(
            activity,
            activity.getString(R.string.permissions_missing_location_msg),
            LOCATION_REQUEST_CODE,
            *perms,
        )
    }

    fun requestBluetoothPermissions(activity: Activity) {
        val perms = getBluetoothPermissions()

        if (EasyPermissions.hasPermissions(activity, *perms)) {
            Log.v(TAG, "Bluetooth permissions already granted.")
            return
        }

        Log.i(TAG, "Requesting missing bluetooth permissions.")
        EasyPermissions.requestPermissions(
            activity,
            activity.getString(R.string.permissions_missing_bt_msg),
            BLUETOOTH_REQUEST_CODE,
            *perms,
        )
    }

    /**
     * Helper to construct the correct permission array based on Android Version
     */
    private fun getBluetoothPermissions(): Array<String> {
        val perms = mutableListOf<String>()

        // On Android 12 (S / API 31) and newer, we need the new runtime permissions.
        // Old permissions (BLUETOOTH, BLUETOOTH_ADMIN) are ignored by the system at runtime.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // For Android 11 and below, Bluetooth permissions are "install-time" (granted automatically).
            // However, we include them here if you want to explicitly check or if EasyPermissions requires a non-empty list.
            // Note: On < Android 12, BLE scanning requires Location permissions, which are handled separately.
            perms.add(Manifest.permission.BLUETOOTH)
            perms.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        return perms.toTypedArray()
    }
}
