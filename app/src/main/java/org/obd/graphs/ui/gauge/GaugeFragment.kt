package org.obd.graphs.ui.gauge

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.ui.preferences.GaugePreferences
import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.getLongSet
import org.obd.metrics.ObdMetric

private const val GAUGE_SELECTED_METRICS_PREF = "pref.gauge.pids.selected"
private const val ENABLE_DRAG_AND_DROP_PREF = "pref.gauge_enable_drag_and_drop"

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
        GaugeViewSetup().configureView(
            viewLifecycleOwner = viewLifecycleOwner,
            recyclerView = root.findViewById(R.id.recycler_view) as RecyclerView,
            metricsIdsPref = GAUGE_SELECTED_METRICS_PREF,
            adapterContext = AdapterContext(
                R.layout.gauge_item
            ),
            enableDragManager = Prefs.getBoolean(ENABLE_DRAG_AND_DROP_PREF, false),
            enableOnTouchListener = enableOnTouchListener,
            adapter = { context: Context,
                        data: MutableList<ObdMetric>,
                        resourceId: Int,
                        height: Int? ->
                GaugeAdapter(context, data, resourceId, height)
            },
            recycleViewPreferences = { GaugePreferences.Serializer() }
        )
    }
}