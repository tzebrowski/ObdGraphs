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
package org.obd.graphs.preferences.dri

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import org.obd.graphs.DiagnosticMappingItem
import org.obd.graphs.R

class EditDiagnosticRequestIdBottomSheet(
    private val existingItem: DiagnosticMappingItem? = null,
    private val onSave: (key: String, value: String, description: String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_dri_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val etDescription = view.findViewById<TextInputEditText>(R.id.etDescription)
        val etKey = view.findViewById<TextInputEditText>(R.id.etKey)
        val etValue = view.findViewById<TextInputEditText>(R.id.etValue)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        if (existingItem != null) {
            tvTitle.text = requireContext().getString(R.string.pref_adapter_diagnostic_request_id_edit_title)
            etDescription.setText(existingItem.description)
            etKey.setText(existingItem.requestKey)
            etValue.setText(existingItem.headerValue)
        } else {
            tvTitle.text = requireContext().getString(R.string.pref_adapter_diagnostic_request_id_add_title)
        }

        btnSave.setOnClickListener {
            val description = etDescription.text.toString().trim()
            val key = etKey.text.toString().trim()
            val value = etValue.text.toString().trim().uppercase()

            if (key.isNotEmpty() && value.isNotEmpty()) {
                onSave(key, value, description)
                dismiss()
            }
        }
    }
}
