package org.obd.graphs.preferences.dtc

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.Nullable
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.preferences.vehicleCapabilitiesManager

class DiagnosticTroubleCodePreferenceDialog : DialogFragment() {
    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        dialog?.let {
            it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            it.window?.requestFeature(Window.FEATURE_NO_TITLE)
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.dialog_dtc, container, false)
        val dtc = vehicleCapabilitiesManager.getDTC()

        if (dtc.isEmpty()){
            dtc.add(resources.getString(R.string.pref_dtc_no_dtc_found))
        }

        val adapter = DiagnosticTroubleCodeViewAdapter(context, dtc)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.adapter = adapter
        return root
    }
}