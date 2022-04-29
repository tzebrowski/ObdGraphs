package org.openobd2.core.logger.ui.dashboard

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.ObdMetric
import org.openobd2.core.logger.R
import org.openobd2.core.logger.ui.common.DragManageAdapter
import org.openobd2.core.logger.ui.common.MetricsViewContext
import org.openobd2.core.logger.ui.common.SwappableAdapter
import org.openobd2.core.logger.ui.common.ToggleToolbarDoubleClickListener
import org.openobd2.core.logger.ui.gauge.GaugeViewSetup
import org.openobd2.core.logger.ui.preferences.DashPreferences

class DashboardFragment : Fragment() {
    lateinit var root: View
    private val dashboardPreferences: DashboardPreferences by lazy { getDashboardPreferences()  }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configureView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_dashboard, container, false)
        configureView()
        return root
    }

    private fun configureView() {

        if (dashboardPreferences.gaugeViewVisible && dashboardPreferences.dashboardViewVisible) {

            configureRecyclerView(R.id.gauge_recycler_view, dashboardPreferences.gaugeViewVisible, 0.25f, 0)
            configureRecyclerView(R.id.dash_recycler_view, dashboardPreferences.dashboardViewVisible, 0.75f, 0)

            setupGaugeRecyclerView(2)
            setupDashRecyclerView()

        } else {
            if (dashboardPreferences.gaugeViewVisible && !dashboardPreferences.dashboardViewVisible) {
                configureRecyclerView(
                    R.id.gauge_recycler_view,
                    true,
                    1f,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                configureRecyclerView(R.id.dash_recycler_view, false, 0f, 0)
                setupGaugeRecyclerView(4)
            }
            if (!dashboardPreferences.gaugeViewVisible && dashboardPreferences.dashboardViewVisible) {
                configureRecyclerView(
                    R.id.dash_recycler_view,
                    true,
                    1f,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                configureRecyclerView(R.id.gauge_recycler_view, false, 0f, 0)
                setupDashRecyclerView()
            }
        }
    }

    private fun configureRecyclerView(id:Int, visible:Boolean, weight: Float, width: Int) {
        val view: RecyclerView = root.findViewById<RecyclerView>(id)
        view.visibility = if (visible) View.VISIBLE else View.GONE
        (view.layoutParams as LinearLayout.LayoutParams).run {
            this.weight = weight
            this.width = width
        }
    }

    private fun setupGaugeRecyclerView(spanCount: Int) {
        GaugeViewSetup.onCreateView(
            viewLifecycleOwner,
            requireContext(),
            root,
            R.id.gauge_recycler_view,
            "pref.dash.gauge_pids.selected",
            R.layout.dashboard_gauge_item,
            spanCount = spanCount)
    }

    private fun setupDashRecyclerView() {
        val metricsViewContext = MetricsViewContext(viewLifecycleOwner,dashboardPreferences.dashboardVisiblePids)

        val sortOrderMap = DashPreferences.SERIALIZER.load(requireContext())?.map {
            it.id to it.position
        }!!.toMap()

        val metrics = metricsViewContext.findMetricsToDisplay(sortOrderMap)
        val spanCount = calculateSpanCount(metrics.size)
        val itemHeight = calculateItemHeight(metrics,spanCount)

        metricsViewContext.adapter = DashboardViewAdapter(root.context, metrics, itemHeight)
        val recyclerView: RecyclerView = root.findViewById(R.id.dash_recycler_view)

        recyclerView.layoutManager = GridLayoutManager(root.context, calculateSpanCount(metrics.size))
        recyclerView.adapter = metricsViewContext.adapter

        val swappableAdapter = object: SwappableAdapter {
            override fun swapItems(fromPosition: Int, toPosition: Int) {
                (metricsViewContext.adapter as DashboardViewAdapter).swapItems(fromPosition, toPosition)
            }

            override fun storePreferences(context: Context) {
                DashPreferences.SERIALIZER.store(context, (metricsViewContext.adapter as DashboardViewAdapter).data)
            }

            override fun deleteItems(fromPosition: Int) {
                val metrics = (metricsViewContext.adapter as DashboardViewAdapter).data
                val itemId: ObdMetric = metrics[fromPosition]
                metrics.remove(itemId)

                updateDashboardPids(metrics.map { obdMetric -> obdMetric.command.pid.id }.toList())

                DashPreferences.SERIALIZER.store(requireContext(), metrics)
                val spanCount = calculateSpanCount(metrics.size)
                val itemHeight = calculateItemHeight(metrics,spanCount)
                metricsViewContext.adapter = DashboardViewAdapter(root.context, metrics, itemHeight)
                val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
                recyclerView.layoutManager =
                    GridLayoutManager(root.context, calculateSpanCount(metrics.size))
                recyclerView.adapter = metricsViewContext.adapter
                recyclerView.refreshDrawableState()

                metricsViewContext.observerMetrics(metrics)
                metricsViewContext.adapter.notifyDataSetChanged()
            }
        }
        val callback = if (dashboardPreferences.swipeToDeleteEnabled)
            DragManageAdapter(
            requireContext(),
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.START or ItemTouchHelper.END, swappableAdapter)
         else
            DragManageAdapter(
            requireContext(),
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.ACTION_STATE_DRAG, swappableAdapter)


        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
        recyclerView.refreshDrawableState()
        recyclerView.addOnItemTouchListener(
            ToggleToolbarDoubleClickListener(
                requireContext()
            )
        )

        metricsViewContext.observerMetrics(metrics)
        metricsViewContext.adapter.notifyDataSetChanged()
    }

    private fun calculateItemHeight(metrics: MutableList<ObdMetric>, spanCount: Int): Int {
        val heightPixels = Resources.getSystem().displayMetrics.heightPixels
        return heightPixels / metrics.size * spanCount
    }

    private fun calculateSpanCount(numberOfItems: Int): Int {
        return if (numberOfItems <= 3) {
            1
        } else {
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1
        }
    }
}