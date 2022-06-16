package org.obd.graphs.ui.gauge

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.obd.graphs.R
import org.obd.graphs.ui.preferences.Prefs

class GaugeFragment : Fragment() {
    private lateinit var root: View

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configureView(false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_gauge, container, false)
        configureView(true)
        return root
    }

    private fun configureView(enableOnTouchListener: Boolean) {
        GaugeViewSetup.onCreateView(
            viewLifecycleOwner,
            requireContext(),
            root,
            R.id.recycler_view,
            "pref.gauge.pids.selected",
            R.layout.gauge_item,
            spanCount = null,
            enableDragManager = Prefs.getBoolean("pref.gauge_enable_drag_and_drop", false),
            enableOnTouchListener = enableOnTouchListener
        )
    }
}