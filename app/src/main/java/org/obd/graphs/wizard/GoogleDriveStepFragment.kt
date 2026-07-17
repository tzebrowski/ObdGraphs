/*
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
package org.obd.graphs.wizard

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import org.obd.graphs.R
import org.obd.graphs.integrations.gcp.gdrive.TripLogDriveManager

class GoogleDriveStepFragment : Fragment(R.layout.fragment_wizard_gdrive) {
    // Constructed in onCreate(), not on click: AuthorizationManager registers an
    // ActivityResultLauncher in its constructor, which Android requires to happen
    // before the fragment reaches STARTED - doing it lazily on click throws.
    private lateinit var driveManager: TripLogDriveManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        driveManager =
            TripLogDriveManager.instance(
                getString(R.string.ANDROID_WEB_CLIENT_ID),
                requireActivity(),
                this
            )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val btnConnect = view.findViewById<MaterialButton>(R.id.btnWizardConnectDrive)
        btnConnect.setOnClickListener {
            btnConnect.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                driveManager.authenticate()
                btnConnect.isEnabled = true
            }
        }
    }
}
