package org.obd.graphs.ui.dashboard

import android.content.Context
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
import org.obd.graphs.ui.common.RecyclerViewSetup
import org.obd.graphs.ui.gauge.AdapterContext
import org.obd.graphs.ui.gauge.GaugeAdapter
import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.getLongSet
import org.obd.metrics.ObdMetric

class DashboardFragment : Fragment() {
    lateinit var root: View
    private val dashboardPreferences: DashboardPreferences by lazy { getDashboardPreferences() }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configureView(false)
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
            viewLifecycleOwner = viewLifecycleOwner,
            recyclerView = root.findViewById(R.id.dashboard_recycler_view) as RecyclerView,
            metricsIdsPref = dashboardPreferences.dashboardSelectedMetrics.first,
            adapterContext = AdapterContext(
                layoutId = R.layout.dashboard_item,
                spanCount = calculateSpanCount(Prefs.getLongSet(dashboardPreferences.dashboardSelectedMetrics.first).size),
                height = calculateHeight(
                    Prefs.getLongSet(dashboardPreferences.dashboardSelectedMetrics.first).size,
                    calculateSpanCount(Prefs.getLongSet(dashboardPreferences.dashboardSelectedMetrics.first).size)
                )
            ),
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
            viewLifecycleOwner,
            root.findViewById(R.id.gauge_recycler_view) as RecyclerView,
            metricsIdsPref = dashboardPreferences.gaugeSelectedMetrics.first,
            adapterContext = AdapterContext(
                layoutId = R.layout.dashboard_gauge_item,
                spanCount = spanCount,
                height = Resources.getSystem().displayMetrics.heightPixels / 3
            ),
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

    private fun calculateHeight(numberOfItems: Int, spanCount: Int): Int {
        val heightPixels = Resources.getSystem().displayMetrics.heightPixels
        val size = if (numberOfItems == 0) 1 else numberOfItems
        return heightPixels / size * spanCount
    }

    private fun calculateSpanCount(numberOfItems: Int): Int {
        return if (numberOfItems <= 3) {
            1
        } else {
            if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1
        }
    }
}