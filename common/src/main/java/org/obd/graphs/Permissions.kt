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
import android.os.Build
import android.util.Log
import org.obd.graphs.commons.R
import pub.devrel.easypermissions.EasyPermissions

private const val TAG = "Permissions"
private const val LOCATION_REQUEST_CODE = 1001
private const val BLUETOOTH_REQUEST_CODE = 1002

object Permissions {
    /**
     * returns TRUE if all required location permissions are granted
     */
    fun hasLocationPermissions(context: Context): Boolean =
        EasyPermissions.hasPermissions(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

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
