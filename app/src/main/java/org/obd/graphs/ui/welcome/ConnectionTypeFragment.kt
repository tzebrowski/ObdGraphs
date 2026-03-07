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

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import org.obd.graphs.R

class ConnectionTypeFragment : Fragment(R.layout.fragment_connection_type) {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Find your MaterialCardViews from the XML layout
        val cardBluetooth = view.findViewById<MaterialCardView>(R.id.cardBluetooth)
        val cardWifi = view.findViewById<MaterialCardView>(R.id.cardWifi)
        val cardUsb = view.findViewById<MaterialCardView>(R.id.cardUsb)

        // Set click listeners for each connection type
        cardBluetooth.setOnClickListener {
            saveConnectionType("BLUETOOTH")
            navigateToNextScreen()
        }

        cardWifi.setOnClickListener {
            saveConnectionType("WIFI")
            navigateToNextScreen()
        }

        cardUsb.setOnClickListener {
            saveConnectionType("USB")
            navigateToNextScreen()
        }
    }

    private fun saveConnectionType(type: String) {
        // Since you have the androidx.preference dependency,
        // SharedPreferences is a great way to save this globally.
        val sharedPrefs = requireActivity().getSharedPreferences("OBD_PREFS", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("CONNECTION_TYPE", type).apply()
    }

    private fun navigateToNextScreen() {
        // Move to the Permissions Fragment using your Navigation Graph action
        findNavController().navigate(R.id.action_connectionType_to_permissions)
    }
}
