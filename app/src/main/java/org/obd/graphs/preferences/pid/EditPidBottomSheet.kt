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
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.obd.graphs.DiagnosticMappingItem
import org.obd.graphs.DiagnosticRequestIDManager
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.JS_ENGINE_NAME
import org.obd.metrics.api.CANNetwork
import org.obd.metrics.pid.ValueType
import javax.script.Compilable
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings

data class PidFormData(
    val description: String?,
    val longDescription: String?,
    val mode: String?,
    val canHeader: String?,
    val canNetwork: CANNetwork?,
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
        val tilFormula = view.findViewById<TextInputLayout>(R.id.tilFormula)
        val tvFormulaRules = view.findViewById<View>(R.id.tvFormulaRules)
        val llToggles = view.findViewById<View>(R.id.llToggles)

        val etDescription = view.findViewById<TextInputEditText>(R.id.etDescription)
        val etLongDescription = view.findViewById<TextInputEditText>(R.id.etLongDescription)
        val etCanHeader = view.findViewById<AutoCompleteTextView>(R.id.etCanHeader)

        val etMode = view.findViewById<AutoCompleteTextView>(R.id.etMode)
        etMode.setupDropdown(listOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "1A", "21", "22"), "01")

        // Suggestions - and the default pre-filled text - show the friendly description, but the
        // raw requestKey (ID) is what actually needs to be saved as the PID's canMode override and
        // matched against Init.Header.mode. The box's displayed text is resolved back through
        // labelToKey both on selection and at save time (extractFormData), so the default label
        // staying untouched still saves the correct ID instead of the literal label text.
        val mappings = DiagnosticRequestIDManager.getMappings()
        val labelToKey = mappings.associate { it.dropdownLabel() to it.requestKey }
        val defaultLabel = mappings.firstOrNull { it.requestKey == "01" }?.dropdownLabel()
        etCanHeader.setupDropdown(labelToKey.keys.toList(), defaultLabel)
        etCanHeader.setOnItemClickListener { parent, _, position, _ ->
            val label = parent.getItemAtPosition(position) as String
            etCanHeader.setText(labelToKey[label] ?: label, false)
        }

        val noneCanNetwork = getString(R.string.pref_pid_alert_can_network_none)
        val etCanNetwork = view.findViewById<AutoCompleteTextView>(R.id.etCanNetwork)
        etCanNetwork.setupDropdown(listOf(noneCanNetwork) + CANNetwork.values().map { it.name }, noneCanNetwork)

        val etValueType = view.findViewById<AutoCompleteTextView>(R.id.etValueType)
        etValueType.setupDropdown(listOf("INT", "DOUBLE", "SHORT"), ValueType.DOUBLE.name)

        val etPidCode = view.findViewById<TextInputEditText>(R.id.etPidCode)
        val etFormula = view.findViewById<TextInputEditText>(R.id.etFormula)

        val etLength = view.findViewById<AutoCompleteTextView>(R.id.etLength)
        etLength.setupDropdown((1..12).map { it.toString() }, "1")

        fun validateFormula() {
            val length = etLength.text.toString().toIntOrNull() ?: 1
            tilFormula.error = if (isFormulaValid(etFormula.text.toString(), length)) {
                null
            } else {
                getString(R.string.pref_pid_alert_formula_validation_error)
            }
        }

        etFormula.doAfterTextChanged { validateFormula() }
        etLength.doAfterTextChanged { validateFormula() }

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
            tvFormulaRules.visibility = View.GONE
            llToggles.visibility = View.GONE

            tvTitle.text = pidItem?.source?.description ?: getString(R.string.pref_pid_manage_dialog_edit_alerts_title)
        } else {
            tilDescription.visibility = View.VISIBLE
            tilLongDescription.visibility = View.VISIBLE
            llCorePidFields.visibility = View.VISIBLE
            llDataFields.visibility = View.VISIBLE
            tilFormula.visibility = View.VISIBLE
            tvFormulaRules.visibility = View.VISIBLE
            llToggles.visibility = View.VISIBLE

            tvTitle.text = if (pidItem != null) getString(R.string.pref_pid_manage_dialog_edit_pid_title) else getString(R.string.pref_pid_manage_dialog_add_new_pid_title)
        }

        if (pidItem != null) {
            val currentCanHeader = pidItem.source.deductMode()
            etCanHeader.setText(mappings.firstOrNull { it.requestKey == currentCanHeader }?.dropdownLabel() ?: currentCanHeader, false)
            etCanNetwork.setText(pidItem.source.overrides?.canNetwork?.name ?: noneCanNetwork, false)
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
            etValueType.setText(pidItem.source.type?.name ?: ValueType.DOUBLE.name, false)

            etLower.setText(pidItem.source.alert.lowerThreshold?.toString() ?: "")
            etUpper.setText(pidItem.source.alert.upperThreshold?.toString() ?: "")
        }

        btnSave.setOnClickListener {
            tvError.visibility = View.GONE

            val lowerVal = etLower.text.toString().toDoubleOrNull()
            val upperVal = etUpper.text.toString().toDoubleOrNull()

            if (lowerVal != null && upperVal != null && lowerVal >= upperVal) {
                tvError.text = getString(R.string.pref_pid_alert_edit_validation_error)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val formulaLength = etLength.text.toString().toIntOrNull() ?: 1
            if (mode.isEdit && !isFormulaValid(etFormula.text.toString(), formulaLength)) {
                tilFormula.error = getString(R.string.pref_pid_alert_formula_validation_error)
                tvError.text = getString(R.string.pref_pid_alert_formula_validation_error)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            val formData = extractFormData(
                etDescription = etDescription,
                etLongDescription = etLongDescription,
                etMode = etMode,
                etCanHeader = etCanHeader,
                canHeaderLabelToKey = labelToKey,
                etCanNetwork = etCanNetwork,
                noneCanNetwork = noneCanNetwork,
                etPidCode = etPidCode,
                etFormula = etFormula,
                etLength = etLength,
                etMin = etMin,
                etMax = etMax,
                etUnits = etUnits,
                cbStable = cbStable,
                cbBatch = cbBatch,
                cbCacheable = cbCacheable,
                lowerVal = lowerVal,
                upperVal = upperVal,
                etValueType = etValueType
            )

            if (mode.isEdit && (
                    formData.description.isNullOrEmpty() ||
                        formData.mode.isNullOrEmpty() ||
                        formData.pidCode.isNullOrEmpty() ||
                        formData.formula.isNullOrEmpty() ||
                        formData.min == null ||
                        formData.max == null
                    )
            ) {
                tvError.text = getString(R.string.pref_pid_manage_dialog_validation_required_fields)
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            onSave(formData)
            dismiss()
        }
    }

    private fun extractFormData(
        etDescription: TextInputEditText,
        etLongDescription: TextInputEditText,
        etMode: AutoCompleteTextView,
        etCanHeader: AutoCompleteTextView,
        canHeaderLabelToKey: Map<String, String>,
        etCanNetwork: AutoCompleteTextView,
        noneCanNetwork: String,
        etPidCode: TextInputEditText,
        etFormula: TextInputEditText,
        etLength: AutoCompleteTextView,
        etMin: TextInputEditText,
        etMax: TextInputEditText,
        etUnits: TextInputEditText,
        cbStable: CheckBox,
        cbBatch: CheckBox,
        cbCacheable: CheckBox,
        lowerVal: Double?,
        upperVal: Double?,
        etValueType: AutoCompleteTextView
    ): PidFormData {
        return PidFormData(
            description = if (mode.isEdit) etDescription.text.toString().trim() else null,
            longDescription = if (mode.isEdit) etLongDescription.text.toString().trim() else null,
            mode = if (mode.isEdit) etMode.text.toString().trim() else null,
            canHeader = if (mode.isEdit) {
                etCanHeader.text.toString().trim().let { canHeaderLabelToKey[it] ?: it }
            } else {
                null
            },
            canNetwork = if (mode.isEdit) {
                val selected = etCanNetwork.text.toString().trim()
                if (selected.isEmpty() || selected == noneCanNetwork) null else CANNetwork.valueOf(selected)
            } else {
                null
            },
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
            valueType = if (mode.isEdit) etValueType.text.toString().trim() else ValueType.DOUBLE.name
        )
    }

    private fun isFormulaValid(formula: String, length: Int): Boolean {
        if (formula.isBlank()) return true

        val engine = ScriptEngineManager().getEngineByName(JS_ENGINE_NAME) as? Compilable ?: return true
        return try {
            val compiled = engine.compile(formula)
            val bindings = SimpleBindings()
            val paramCount = length.coerceIn(1, 26)
            ('A' until 'A' + paramCount).forEach { bindings[it.toString()] = 0 }
            compiled.eval(bindings)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun DiagnosticMappingItem.dropdownLabel(): String =
        if (displayName == requestKey) requestKey else "$displayName ($requestKey)"

    private fun AutoCompleteTextView.setupDropdown(
        items: List<String>,
        defaultValue: String? = null
    ) {
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, items)
        setAdapter(adapter)
        defaultValue?.let { setText(it, false) }
    }
}
