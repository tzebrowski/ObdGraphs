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
package org.obd.graphs.preferences.pid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import org.obd.graphs.R

class EditPidBottomSheet(
    private val pidItem: PidDefinitionDetails,
    private val onSave: (PidDefinitionDetails) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_pid_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val etFormula = view.findViewById<TextInputEditText>(R.id.etFormula)
        val etLower = view.findViewById<TextInputEditText>(R.id.etLower)
        val etUpper = view.findViewById<TextInputEditText>(R.id.etUpper)
        val tvError = view.findViewById<TextView>(R.id.tvValidationError)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        tvTitle.text = pidItem.source.description
        etFormula.setText(pidItem.source.formula)
        etLower.setText(pidItem.source.alert.lowerThreshold?.toString() ?: "")
        etUpper.setText(pidItem.source.alert.upperThreshold?.toString() ?: "")

        btnSave.setOnClickListener {
            val formulaStr = etFormula.text.toString().trim()
            val lowerVal = etLower.text.toString().toIntOrNull()
            val upperVal = etUpper.text.toString().toIntOrNull()

            if (lowerVal != null && upperVal != null && lowerVal >= upperVal) {
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            pidItem.source.formula = formulaStr
            pidItem.source.alert.lowerThreshold = lowerVal
            pidItem.source.alert.upperThreshold = upperVal

            onSave(pidItem)
            dismiss()
        }
    }
}
