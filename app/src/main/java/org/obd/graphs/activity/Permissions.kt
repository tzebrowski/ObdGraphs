/**
 * Copyright 2019-2023, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.obd.graphs.activity

import android.Manifest
import android.app.Activity
import android.os.Build
import android.util.Log
import org.obd.graphs.R
import pub.devrel.easypermissions.EasyPermissions

internal val permissions = Permissions()

internal class Permissions {
    fun requestLocationPermissions(activity: Activity) {
        Log.d(
            LOG_TAG,
            "Has permission to ACCESS_COARSE_LOCATION ${
                EasyPermissions.hasPermissions(
                    activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }"
        )

        Log.d(
            LOG_TAG,
            "Has permission to ACCESS_FINE_LOCATION ${
                EasyPermissions.hasPermissions(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }"
        )

        val perms = mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        EasyPermissions.requestPermissions(
            activity,
            activity.resources.getString(R.string.permissions_missing_bt_msg),
            1,
            *perms.toTypedArray()
        )
    }

    fun requestBluetoothPermissions(activity: Activity) {

        Log.d(
            LOG_TAG,
            "Has permission to BLUETOOTH_ADMIN ${
                EasyPermissions.hasPermissions(
                    activity,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            }"
        )
        Log.d(
            LOG_TAG,
            "Has permission to BLUETOOTH ${
                EasyPermissions.hasPermissions(
                    activity,
                    Manifest.permission.BLUETOOTH
                )
            }"
        )

        val perms = mutableListOf(
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH
        )

        if (Build.VERSION.SDK_INT > 30) {
            Log.d(
                LOG_TAG,
                "Has permission to BLUETOOTH_CONNECT ${
                    EasyPermissions.hasPermissions(
                        activity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                }"
            )
            Log.d(
                LOG_TAG,
                "Has permission to BLUETOOTH_SCAN ${
                    EasyPermissions.hasPermissions(
                        activity,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                }"
            )
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        EasyPermissions.requestPermissions(
            activity,
            activity.resources.getString(R.string.permissions_missing_bt_msg),
            1,
            *perms.toTypedArray()
        )
    }
}