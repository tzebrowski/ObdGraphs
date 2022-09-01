package org.obd.graphs.activity

import android.Manifest
import android.os.Build
import android.util.Log
import org.obd.graphs.R
import pub.devrel.easypermissions.EasyPermissions

fun MainActivity.requestBluetoothPermissions() {
    Log.d(
        ACTIVITY_LOGGER_TAG,
        "Has permission to BLUETOOTH_ADMIN ${
            EasyPermissions.hasPermissions(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }"
    )
    Log.d(
        ACTIVITY_LOGGER_TAG,
        "Has permission to BLUETOOTH ${
            EasyPermissions.hasPermissions(
                this,
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
            ACTIVITY_LOGGER_TAG,
            "Has permission to BLUETOOTH_CONNECT ${
                EasyPermissions.hasPermissions(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }"
        )
        Log.d(
            ACTIVITY_LOGGER_TAG,
            "Has permission to BLUETOOTH_SCAN ${
                EasyPermissions.hasPermissions(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            }"
        )
        perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        perms.add(Manifest.permission.BLUETOOTH_SCAN)
    }

    EasyPermissions.requestPermissions(
        this,
        resources.getString(R.string.permissions_missing_bt_msg),
        1,
        *perms.toTypedArray()
    )
}