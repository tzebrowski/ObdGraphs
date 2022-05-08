package org.openobd2.core.logger.ui.gauge

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.openobd2.core.logger.R
import org.openobd2.core.logger.ui.preferences.Prefs

class GaugeFragment : Fragment() {
    private lateinit var root: View

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configureView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_gauge, container, false)
        configureView()
        return root
    }

    private fun configureView() {
        GaugeViewSetup.onCreateView(
            viewLifecycleOwner,
            requireContext(),
            root,
            R.id.recycler_view,
            "pref.gauge.pids.selected",
            R.layout.gauge_item,
            spanCount = null,
            enableDragManager =   Prefs.getBoolean("pref.gauge_enable_drag_and_drop",false)
        )
    }
}