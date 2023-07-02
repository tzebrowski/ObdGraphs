package org.obd.graphs.activity

import android.Manifest
import android.app.Activity
import android.os.Build
import android.util.Log
import org.obd.graphs.R
import pub.devrel.easypermissions.EasyPermissions

val permissions = Permissions()

class Permissions {
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