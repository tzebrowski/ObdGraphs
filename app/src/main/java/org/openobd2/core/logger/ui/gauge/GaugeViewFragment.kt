package org.openobd2.core.logger.ui.gauge

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.openobd2.core.logger.R

class GaugeViewFragment : Fragment() {
    lateinit var root: View

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        root.let {
            val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
            recyclerView.layoutManager = GridLayoutManager(root.context, spanCount())
            recyclerView.refreshDrawableState()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_gauge, container, false)
        GaugeViewSetup().onCreateView(viewLifecycleOwner,requireContext(),root,spanCount(), R.id.recycler_view,"pref.gauge.pids.selected")
        return root
    }

    private fun spanCount(): Int {
        return if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2
    }
}