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
import org.obd.graphs.ui.common.isTablet
import org.obd.graphs.ui.recycler.RecyclerViewSetup
import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.getLongSet
import org.obd.metrics.ObdMetric
import kotlin.math.roundToInt

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
        RecyclerViewSetup().configureView(
            viewLifecycleOwner = viewLifecycleOwner,
            recyclerView = root.findViewById(R.id.recycler_view) as RecyclerView,
            metricsIdsPref = GAUGE_SELECTED_METRICS_PREF,
            adapterContext = AdapterContext(
                layoutId = R.layout.gauge_item,
                spanCount = calculateSpan(
                    requireContext(),
                    Prefs.getLongSet(GAUGE_SELECTED_METRICS_PREF).size
                )
            ),
            enableSwipeToDelete = Prefs.getBoolean(ENABLE_DRAG_AND_DROP_PREF, false),
            enableDragManager = Prefs.getBoolean(ENABLE_DRAG_AND_DROP_PREF, false),
            enableOnTouchListener = enableOnTouchListener,
            adapter = { context: Context,
                        data: MutableList<ObdMetric>,
                        resourceId: Int,
                        height: Int? ->
                GaugeAdapter(context, data, resourceId, height)
            },
            metricsSerializerPref = "prefs.gauge.pids.settings"
        )
    }

    private fun calculateSpan(context: Context, numberOfItems: Int): Int {
        return when (isTablet(context)) {
            false -> {
                return if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    3
                } else {
                    2
                }
            }
            else -> {
                when (numberOfItems) {
                    0 -> 1
                    2 -> 2
                    1 -> 1
                    else -> (numberOfItems / 2.0).roundToInt()
                }
            }
        }
    }
}