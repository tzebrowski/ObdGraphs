package org.openobd2.core.logger.ui.dash

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import org.obd.metrics.Metric
import org.obd.metrics.command.obd.ObdCommand
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.DataLogger
import org.openobd2.core.logger.bl.ModelChangePublisher
import org.openobd2.core.logger.bl.NOTIFICATION_ERROR
import org.openobd2.core.logger.ui.dash.RecyclerItemDoubleClickListener.OnItemDoubleClickListener
import org.openobd2.core.logger.ui.preferences.DashItemPreferences
import org.openobd2.core.logger.ui.preferences.Preferences


internal class DragManageAdapter(
    ctx: Context,
    adapter: DashViewAdapter,
    dragDirs: Int,
    swipeDirs: Int
) :
    ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {
    private var dashViewAdapter = adapter
    private var context = ctx

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        dashViewAdapter.swapItems(viewHolder.adapterPosition, target.adapterPosition)
        DashItemPreferences.store(context, dashViewAdapter.mData)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
    }

}

class RecyclerItemDoubleClickListener(
    context: Context?,
    private val doubleClickListener: OnItemDoubleClickListener?
) :
    OnItemTouchListener {

    interface OnItemDoubleClickListener {
        fun onItemDoubleClick(view: View?, position: Int)
    }

    var mGestureDetector: GestureDetector
    override fun onInterceptTouchEvent(view: RecyclerView, e: MotionEvent): Boolean {
        val childView = view.findChildViewUnder(e.x, e.y)
        if (childView != null && doubleClickListener != null && mGestureDetector.onTouchEvent(e)) {
            doubleClickListener.onItemDoubleClick(childView, view.getChildPosition(childView))
        }
        return false
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onTouchEvent(view: RecyclerView, motionEvent: MotionEvent) {}

    init {
        mGestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                return true
            }
        })
    }
}

val  TOGGLE_TOOLBAR_ACTION: String = "TOGGLE_TOOLBAR"

class DashFragment : Fragment() {

    lateinit var root: View

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        root.let {
            setupRecyclerView()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        root = inflater.inflate(R.layout.fragment_dash, container, false)
        setupRecyclerView()
        return root
    }

    private fun setupRecyclerView() {
        val selectedPids = Preferences.getStringSet(requireContext(), "pref.dash.pids.selected")
        val data = DataLogger.INSTANCE.buildMetricsBy(selectedPids)

        val metricsPreferences = DashItemPreferences.load(requireContext())?.map {
            it.query to it.position
        }!!.toMap()

        data.sortWith(Comparator { m1: Metric<*>, m2: Metric<*> ->
            if (metricsPreferences.containsKey(m1.command.query) && metricsPreferences.containsKey(m2.command.query)) {
                metricsPreferences[m1.command.query]!!
                    .compareTo(metricsPreferences[m2.command.query]!!)
            } else {
                -1
            }
        })

        val adapter = DashViewAdapter(root.context, data)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)

        recyclerView.layoutManager = GridLayoutManager(root.context, spanCount())
        recyclerView.adapter = adapter

        val callback = DragManageAdapter(
            requireContext(),
            adapter,
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.START or ItemTouchHelper.END
        )
        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(recyclerView)

        adapter.notifyDataSetChanged()

        ModelChangePublisher.liveData.observe(viewLifecycleOwner, Observer {
            if (selectedPids.contains((it.command as ObdCommand).pid.pid)) {
                val indexOf = data.indexOf(it)
                if (indexOf == -1) {
                    data.add(it)
                    adapter.notifyItemInserted(data.indexOf(it))
                } else {
                    data[indexOf] = it
                    adapter.notifyItemChanged(indexOf, it)
                }
            }
        })
        recyclerView.refreshDrawableState()

        recyclerView.addOnItemTouchListener(
            RecyclerItemDoubleClickListener(
                requireContext(), object : OnItemDoubleClickListener {
                    override fun onItemDoubleClick(view: View?, position: Int) {
                        requireContext().sendBroadcast(Intent().apply {
                            action = TOGGLE_TOOLBAR_ACTION
                        })
                    }
                })
        )
    }

    private fun spanCount(): Int {
        return if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1
    }
}