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
package org.obd.graphs.ui.welcome

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import pub.devrel.easypermissions.EasyPermissions
import org.obd.graphs.R // Make sure this matches your project's R file

class PermissionsFragment : Fragment(R.layout.fragment_permissions), EasyPermissions.PermissionCallbacks {

    companion object {
        private const val RC_OBD_PERMISSIONS = 123
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnGrantPermissions = view.findViewById<Button>(R.id.btnGrantPermissions)

        if (hasRequiredPermissions()) {
            navigateToScanning()
        }

        btnGrantPermissions.setOnClickListener {
            requestObdPermissions()
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION // Often still needed for Wi-Fi scanning
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return EasyPermissions.hasPermissions(requireContext(), *getRequiredPermissions())
    }

    private fun requestObdPermissions() {
        val perms = getRequiredPermissions()

        if (hasRequiredPermissions()) {
            navigateToScanning()
        } else {
            EasyPermissions.requestPermissions(
                this,
                "To find and connect to your OBD adapter, this app needs Bluetooth and Location access.",
                RC_OBD_PERMISSIONS,
                *perms
            )
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        navigateToScanning()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        // Optional: If the user permanently denied the permission, you can show a dialog
        // directing them to the Android System Settings using EasyPermissions.
        /*
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
        */
    }

    private fun navigateToScanning() {
        // Move to the Scanning Fragment
        findNavController().navigate(R.id.action_permissions_to_scanning)
    }
}
