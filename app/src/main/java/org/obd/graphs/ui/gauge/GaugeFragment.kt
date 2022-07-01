package org.obd.graphs.ui.gauge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
private const val ENABLE_SWIPE_TO_DELETE_PREF = "pref.gauge.swipe_to_delete"
private const val CONFIGURE_CHANGE_EVENT_GAUGE = "recycler.view.change.configuration.event.gauge_id"

class GaugeFragment : Fragment() {
    private lateinit var root: View

    private var configurationChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            configureView(false)
        }
    }

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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.registerReceiver(configurationChangedReceiver, IntentFilter().apply {
            addAction(CONFIGURE_CHANGE_EVENT_GAUGE)
        })
    }

    override fun onDetach() {
        super.onDetach()
        activity?.unregisterReceiver(configurationChangedReceiver)
    }

    private fun configureView(enableOnTouchListener: Boolean) {
        RecyclerViewSetup().configureView(
            configureChangeEventId = CONFIGURE_CHANGE_EVENT_GAUGE,
            viewLifecycleOwner = viewLifecycleOwner,
            recyclerView = root.findViewById(R.id.recycler_view) as RecyclerView,
            metricsIdsPref = GAUGE_SELECTED_METRICS_PREF,
            adapterContext = AdapterContext(
                layoutId = R.layout.gauge_item,
                spanCount = calculateSpan()
            ),

            enableSwipeToDelete = Prefs.getBoolean(ENABLE_SWIPE_TO_DELETE_PREF, false),
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

    private fun calculateSpan(): Int {
        return when (isTablet()) {
            false -> {
                return if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    3
                } else {
                    2
                }
            }
            else -> {
                when (val numberOfItems =  Prefs.getLongSet(GAUGE_SELECTED_METRICS_PREF).size) {
                    0 -> 1
                    2 -> 2
                    1 -> 1
                    else -> (numberOfItems / 2.0).roundToInt()
                }
            }
        }
    }
}