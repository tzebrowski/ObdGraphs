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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import org.obd.graphs.DiagnosticRequestIDManager
import org.obd.graphs.R

data class PidFormData(
    val description: String?,
    val longDescription: String?,
    val mode: String?,
    val canHeader: String?,
    val pidCode: String?,
    val formula: String?,
    val length: Int,
    val min: Number?,
    val max: Number?,
    val units: String?,
    val stable: Boolean,
    val cacheable: Boolean,
    val valueType: String,
    val batch: Boolean,
    val lowerThreshold: Number?,
    val upperThreshold: Number?
)

class EditPidBottomSheet(
    private val pidItem: PidDefinitionDetails? = null,
    private val mode: PidDefinitionDialogMode = PidDefinitionDialogMode.Alert,
    private val onSave: (PidFormData) -> Unit
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

        val tilDescription = view.findViewById<View>(R.id.tilDescription)
        val tilLongDescription = view.findViewById<View>(R.id.tilLongDescription)
        val llCorePidFields = view.findViewById<View>(R.id.llCorePidFields)
        val llDataFields = view.findViewById<View>(R.id.llDataFields)
        val tilFormula = view.findViewById<View>(R.id.tilFormula)
        val llToggles = view.findViewById<View>(R.id.llToggles)

        val etDescription = view.findViewById<TextInputEditText>(R.id.etDescription)
        val etLongDescription = view.findViewById<TextInputEditText>(R.id.etLongDescription)
        val etCanHeader = view.findViewById<AutoCompleteTextView>(R.id.etCanHeader)

        val etMode = view.findViewById<AutoCompleteTextView>(R.id.etMode)
        val modes = listOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "1A", "21", "22")
        val modeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            modes
        )

        etMode.setAdapter(modeAdapter)
        etMode.setText("01", false)

        val canHeaders = DiagnosticRequestIDManager.getMappings().map { it.requestKey }
        val canHEaderAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            canHeaders
        )
        etCanHeader.setAdapter(canHEaderAdapter)

        val etValueType = view.findViewById<AutoCompleteTextView>(R.id.etValueType)
        val valueTypes = listOf("INT", "DOUBLE", "SHORT")
        val valueTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            valueTypes
        )
        etValueType.setAdapter(valueTypeAdapter)

        val etPidCode = view.findViewById<TextInputEditText>(R.id.etPidCode)
        val etFormula = view.findViewById<TextInputEditText>(R.id.etFormula)

        val etLength = view.findViewById<AutoCompleteTextView>(R.id.etLength)
        val lengthAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            (1..12).map { it.toString() }
        )
        etLength.setAdapter(lengthAdapter)
        etLength.setText("1", false)

        val etMin = view.findViewById<TextInputEditText>(R.id.etMin)
        val etMax = view.findViewById<TextInputEditText>(R.id.etMax)
        val etUnits = view.findViewById<TextInputEditText>(R.id.etUnits)
        val cbStable = view.findViewById<CheckBox>(R.id.cbStable)
        val cbCacheable = view.findViewById<CheckBox>(R.id.cbCacheable)
        val cbBatch = view.findViewById<CheckBox>(R.id.cbBatch)

        val etLower = view.findViewById<TextInputEditText>(R.id.etLower)
        val etUpper = view.findViewById<TextInputEditText>(R.id.etUpper)

        val tvError = view.findViewById<TextView>(R.id.tvValidationError)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        if (mode.isAlert) {
            tilDescription.visibility = View.GONE
            tilLongDescription.visibility = View.GONE
            llCorePidFields.visibility = View.GONE
            llDataFields.visibility = View.GONE
            tilFormula.visibility = View.GONE
            llToggles.visibility = View.GONE

            tvTitle.text = pidItem?.source?.description ?: getString(R.string.pref_pid_manage_dialog_edit_alerts_title)
        } else {
            tilDescription.visibility = View.VISIBLE
            tilLongDescription.visibility = View.VISIBLE
            llCorePidFields.visibility = View.VISIBLE
            llDataFields.visibility = View.VISIBLE
            tilFormula.visibility = View.VISIBLE
            llToggles.visibility = View.VISIBLE

            tvTitle.text = if (pidItem != null) getString(R.string.pref_pid_manage_dialog_edit_pid_title) else getString(R.string.pref_pid_manage_dialog_add_new_pid_title)
        }

        if (pidItem != null) {
            etCanHeader.setText(pidItem.source.deductMode())

            etMode.setText(pidItem.source.mode, false)

            etDescription.setText(pidItem.source.description)
            etLongDescription.setText(pidItem.source.longDescription)
            etPidCode.setText(pidItem.source.pid)
            etFormula.setText(pidItem.source.formula)
            etLength.setText(pidItem.source.length.toString(), false)
            etMin.setText(pidItem.source.min?.toString() ?: "")
            etMax.setText(pidItem.source.max?.toString() ?: "")
            etUnits.setText(pidItem.source.units ?: "")
            cbStable.isChecked = pidItem.source.stable ?: true
            cbCacheable.isChecked = pidItem.source.cacheable ?: true
            etValueType.setText(pidItem.source.type?.name ?: "DOUBLE", false)

            etLower.setText(pidItem.source.alert.lowerThreshold?.toString() ?: "")
            etUpper.setText(pidItem.source.alert.upperThreshold?.toString() ?: "")
        }

        btnSave.setOnClickListener {
            val lowerVal = etLower.text.toString().toDoubleOrNull()
            val upperVal = etUpper.text.toString().toDoubleOrNull()

            if (lowerVal != null && upperVal != null && lowerVal >= upperVal) {
                tvError.text = getString(R.string.pref_pid_alert_edit_validation_error)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val formData = PidFormData(
                description = if (mode.isEdit) etDescription.text.toString().trim() else null,
                longDescription = if (mode.isEdit) etLongDescription.text.toString().trim() else null,

                mode = if (mode.isEdit) etMode.text.toString().trim() else null,

                canHeader = if (mode.isEdit) etCanHeader.text.toString().trim() else null,
                pidCode = if (mode.isEdit) etPidCode.text.toString().trim() else null,
                formula = if (mode.isEdit) etFormula.text.toString().trim() else null,
                length = etLength.text.toString().toIntOrNull() ?: 1,
                min = etMin.text.toString().toDoubleOrNull(),
                max = etMax.text.toString().toDoubleOrNull(),
                units = etUnits.text.toString().trim(),
                stable = cbStable.isChecked,
                batch = cbBatch.isChecked,
                cacheable = cbCacheable.isChecked,
                lowerThreshold = lowerVal,
                upperThreshold = upperVal,
                valueType = if (mode.isEdit) etValueType.text.toString().trim() else "DOUBLE"
            )

            if (mode.isEdit && (formData.description.isNullOrEmpty() || formData.mode.isNullOrEmpty() || formData.pidCode.isNullOrEmpty())) {
                tvError.text = getString(R.string.pref_pid_manage_dialog_validation_required_fields)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            onSave(formData)
            dismiss()
        }
    }
}
