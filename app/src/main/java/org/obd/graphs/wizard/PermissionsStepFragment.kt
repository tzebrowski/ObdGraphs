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
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import org.obd.graphs.Permissions
import org.obd.graphs.R

class PermissionsStepFragment : Fragment(R.layout.fragment_wizard_permissions) {
    private lateinit var tvStatus: TextView
    private lateinit var btnGrant: MaterialButton

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        tvStatus = view.findViewById(R.id.tvWizardPermissionsStatus)
        btnGrant = view.findViewById(R.id.btnWizardGrantPermissions)

        btnGrant.setOnClickListener {
            Permissions.showPermissionOnboarding(requireActivity(), onDeclined = { refreshStatus() })
        }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    fun refreshStatus() {
        if (!isAdded) return

        val granted = !Permissions.isAnyPermissionMissing(requireContext())
        tvStatus.setText(
            if (granted) R.string.wizard_step_permissions_status_granted else R.string.wizard_step_permissions_status_missing
        )
        btnGrant.visibility = if (granted) View.GONE else View.VISIBLE
    }
}
