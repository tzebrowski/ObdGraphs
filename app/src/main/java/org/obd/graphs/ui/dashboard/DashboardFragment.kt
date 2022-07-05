package org.obd.graphs.ui.dashboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.ui.recycler.RecyclerViewSetup
import org.obd.graphs.ui.gauge.AdapterContext
import org.obd.graphs.ui.gauge.GaugeAdapter
import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.getLongSet
import org.obd.metrics.api.model.ObdMetric

private const val CONFIGURATION_CHANGE_EVENT_GAUGE = "recycler.view.change.configuration.event.dash_gauge_id"
private const val CONFIGURATION_CHANGE_EVENT_DASH = "recycler.view.change.configuration.event.dash_id"

class DashboardFragment : Fragment() {
    lateinit var root: View

    private val dashboardPreferences: DashboardPreferences by lazy { getDashboardPreferences() }
    private var configurationChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
          configureView(false)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configureView(false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.registerReceiver(configurationChangedReceiver, IntentFilter().apply {
            addAction(CONFIGURATION_CHANGE_EVENT_DASH)
            addAction(CONFIGURATION_CHANGE_EVENT_GAUGE)
        })
    }

    override fun onDetach() {
        super.onDetach()
        activity?.unregisterReceiver(configurationChangedReceiver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_dashboard, container, false)
        configureView(true)
        return root
    }

    private fun configureView(enableOnTouchListener: Boolean) {

        if (dashboardPreferences.gaugeViewVisible && dashboardPreferences.dashboardViewVisible) {

            configureRecyclerView(
                R.id.gauge_recycler_view,
                dashboardPreferences.gaugeViewVisible,
                0.25f,
                0
            )
            configureRecyclerView(
                R.id.dashboard_recycler_view,
                dashboardPreferences.dashboardViewVisible,
                0.75f,
                0
            )

            setupGaugeRecyclerView(1, enableOnTouchListener)
            setupDashboardRecyclerView(enableOnTouchListener)

        } else {
            if (dashboardPreferences.gaugeViewVisible && !dashboardPreferences.dashboardViewVisible) {
                configureRecyclerView(
                    R.id.gauge_recycler_view,
                    true,
                    1f,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                configureRecyclerView(R.id.dashboard_recycler_view, false, 0f, 0)
                setupGaugeRecyclerView(2, enableOnTouchListener)
            }
            if (!dashboardPreferences.gaugeViewVisible && dashboardPreferences.dashboardViewVisible) {
                configureRecyclerView(
                    R.id.dashboard_recycler_view,
                    true,
                    1f,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                configureRecyclerView(R.id.gauge_recycler_view, false, 0f, 0)
                setupDashboardRecyclerView(enableOnTouchListener)
            }
        }
    }

    private fun configureRecyclerView(id: Int, visible: Boolean, weight: Float, width: Int) {
        val view: RecyclerView = root.findViewById(id)
        view.visibility = if (visible) View.VISIBLE else View.GONE
        (view.layoutParams as LinearLayout.LayoutParams).run {
            this.weight = weight
            this.width = width
        }
    }

    private fun setupDashboardRecyclerView(enableOnTouchListener: Boolean) {
        RecyclerViewSetup().configureView(
            configureChangeEventId = CONFIGURATION_CHANGE_EVENT_DASH,
            viewLifecycleOwner = viewLifecycleOwner,
            recyclerView = root.findViewById(R.id.dashboard_recycler_view) as RecyclerView,
            metricsIdsPref = dashboardPreferences.dashboardSelectedMetrics.first,
            adapterContext = AdapterContext(
                layoutId = R.layout.dashboard_item,
                spanCount = calculateSpanCount(),
                height = calculateHeight(Prefs.getLongSet(dashboardPreferences.dashboardSelectedMetrics.first).size)
            ),
            enableDragManager = dashboardPreferences.dragAndDropEnabled,
            enableOnTouchListener = enableOnTouchListener,
            enableSwipeToDelete = dashboardPreferences.swipeToDeleteEnabled,
            adapter = { context: Context,
                        data: MutableList<ObdMetric>,
                        resourceId: Int,
                        height: Int? ->
                DashboardViewAdapter(context, data, resourceId, height)
            },
            metricsSerializerPref = "prefs.dash.pids.settings"
        )
    }


    private fun setupGaugeRecyclerView(spanCount: Int, enableOnTouchListener: Boolean) {

        RecyclerViewSetup().configureView(
            configureChangeEventId = CONFIGURATION_CHANGE_EVENT_GAUGE,
            viewLifecycleOwner = viewLifecycleOwner,
            recyclerView = root.findViewById(R.id.gauge_recycler_view) as RecyclerView,
            metricsIdsPref = dashboardPreferences.gaugeSelectedMetrics.first,
            adapterContext = AdapterContext(
                layoutId = R.layout.dashboard_gauge_item,
                spanCount = spanCount,
                height = Resources.getSystem().displayMetrics.heightPixels / 3
            ),
            enableDragManager = dashboardPreferences.dragAndDropEnabled,
            enableOnTouchListener = enableOnTouchListener,
            enableSwipeToDelete = dashboardPreferences.swipeToDeleteEnabled,
            adapter = { context: Context,
                        data: MutableList<ObdMetric>,
                        resourceId: Int,
                        height: Int? ->
                GaugeAdapter(context, data, resourceId, height)
            },
            metricsSerializerPref = "prefs.gauge.pids.settings"
        )
    }

    private fun calculateHeight(numberOfItems: Int): Int {
        val spanCount = calculateSpanCount()
        val heightPixels = Resources.getSystem().displayMetrics.heightPixels
        val size = if (numberOfItems == 0) 1 else numberOfItems
        return heightPixels / size * spanCount
    }

    private fun calculateSpanCount(): Int {
        val numberOfItems =  Prefs.getLongSet(dashboardPreferences.dashboardSelectedMetrics.first).size
        return if (numberOfItems <= 3) {
            1
        } else {
            if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1
        }
    }
}