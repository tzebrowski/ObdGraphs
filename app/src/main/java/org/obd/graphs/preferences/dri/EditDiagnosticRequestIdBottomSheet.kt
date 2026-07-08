package org.obd.graphs.preferences.dri

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import org.obd.graphs.R

class EditDiagnosticRequestIdBottomSheet(
    private val existingItem: DiagnosticMappingItem? = null,
    private val onSave: (key: String, value: String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_diagnostic_request_id, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val etKey = view.findViewById<TextInputEditText>(R.id.etKey)
        val etValue = view.findViewById<TextInputEditText>(R.id.etValue)
        val btnSave = view.findViewById<Button>(R.id.btnSave)

        if (existingItem != null) {
            tvTitle.text = "Edit Request ID Mapping"
            etKey.setText(existingItem.requestKey)
            etValue.setText(existingItem.headerValue)
        } else {
            tvTitle.text = "Add New Request ID Mapping"
        }

        btnSave.setOnClickListener {
            val key = etKey.text.toString().trim()
            val value = etValue.text.toString().trim().uppercase()

            if (key.isNotEmpty() && value.isNotEmpty()) {
                onSave(key, value)
                dismiss()
            }
        }
    }
}
