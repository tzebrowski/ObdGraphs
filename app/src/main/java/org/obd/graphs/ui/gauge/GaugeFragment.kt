package org.obd.graphs.ui.gauge

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTING_EVENT
import org.obd.graphs.preferences.*
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.TOGGLE_TOOLBAR_ACTION
import org.obd.graphs.ui.common.isTablet
import org.obd.graphs.ui.recycler.RecyclerViewSetup
import org.obd.graphs.ui.recycler.SimpleAdapter
import org.obd.metrics.api.model.ObdMetric
import kotlin.math.roundToInt

private const val ENABLE_DRAG_AND_DROP_PREF = "pref.gauge_enable_drag_and_drop"
private const val ENABLE_SWIPE_TO_DELETE_PREF = "pref.gauge.swipe_to_delete"
private const val CONFIGURE_CHANGE_EVENT_GAUGE = "recycler.view.change.configuration.event.gauge_id"
private const val GAUGE_PIDS_SETTINGS = "prefs.gauge.pids.settings"

class GaugeFragment : Fragment() {
    private lateinit var root: View

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                TOGGLE_TOOLBAR_ACTION -> {
                    val virtualScreenPanel = root.findViewById<LinearLayout>(R.id.virtual_view_panel)
                    virtualScreenPanel.isVisible =  !virtualScreenPanel.isVisible
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private var configurationChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                CONFIGURE_CHANGE_EVENT_GAUGE -> {
                    configureView(false)
                }
                DATA_LOGGER_CONNECTING_EVENT -> {
                    val recyclerView = root.findViewById(R.id.recycler_view) as RecyclerView
                    val adapter = recyclerView.adapter as SimpleAdapter
                    val metrics = RecyclerViewSetup().prepareMetrics(
                        metricsIdsPref = getVirtualScreenMetrics(),
                        metricsSerializerPref = GAUGE_PIDS_SETTINGS
                    )
                    adapter.data.clear()
                    adapter.data.addAll(metrics)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireContext().unregisterReceiver(broadcastReceiver)
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
        setupVirtualViewPanel()

             registerReceivers()

        return root
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.registerReceiver(configurationChangedReceiver, IntentFilter().apply {
            addAction(CONFIGURE_CHANGE_EVENT_GAUGE)
            addAction(DATA_LOGGER_CONNECTING_EVENT)
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
            metricsIdsPref = getVirtualScreenMetrics(),
            adapterContext = AdapterContext(
                layoutId = R.layout.item_gauge,
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
            metricsSerializerPref = GAUGE_PIDS_SETTINGS
        )
    }

    private fun calculateSpan(): Int {
        return when (isTablet()) {
            false -> {
                return if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    3
                } else {
                    when (Prefs.getLongSet(getVirtualScreenMetrics()).size) {
                        0 -> 1
                        1 -> 1
                        2 -> 1
                        else -> 2
                    }
                }
            }
            else -> {
                when (val numberOfItems = Prefs.getLongSet(getVirtualScreenMetrics()).size) {
                    0 -> 1
                    2 -> 2
                    1 -> 1
                    else -> (numberOfItems / 2.0).roundToInt()
                }
            }
        }
    }
    private fun setVirtualViewBtn (btnId: Int, selection: String, viewId: String){
        (root.findViewById<Button>(btnId)).let {
            if (selection == viewId) {
                it.setBackgroundColor(COLOR_CARDINAL)
            } else {
                it.setBackgroundColor(COLOR_PHILIPPINE_GREEN)
            }

            it.setOnClickListener {
                Prefs.updateString(VIRTUAL_SCREEN_SELECTION, viewId)
                configureView(true)
                setupVirtualViewPanel()
            }
        }
    }

    private fun setupVirtualViewPanel() {
        val selection = getCurrentVirtualScreen()
        setVirtualViewBtn(R.id.virtual_view_1,selection,"1")
        setVirtualViewBtn(R.id.virtual_view_2,selection,"2")
        setVirtualViewBtn(R.id.virtual_view_3,selection,"3")
        setVirtualViewBtn(R.id.virtual_view_4,selection,"4")
        setVirtualViewBtn(R.id.virtual_view_5,selection,"5")
    }
    private fun registerReceivers() {
        requireContext().registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(TOGGLE_TOOLBAR_ACTION)
        })
    }
}