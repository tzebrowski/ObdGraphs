package org.obd.graphs.ui.metrics

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.CarMetricsCollector
import org.obd.graphs.R
import org.obd.graphs.RenderingThread
import org.obd.graphs.bl.datalogger.*
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import org.obd.graphs.ui.common.ToggleToolbarDoubleClickListener

class MetricsFragment : Fragment() {
    private val metricsCollector = CarMetricsCollector()
    private lateinit var root: View

    private val renderingThread: RenderingThread = RenderingThread(
        renderAction = {
            val adapter = ((root.findViewById(R.id.recycler_view) as RecyclerView).adapter) as MetricsViewAdapter
            val data = adapter.data
            metricsCollector.metrics().forEach {
                it.run {
                    val indexOf = data.indexOf(it)
                    if (indexOf == -1) {
                        data.add(it)
                        adapter.notifyItemInserted(data.indexOf(it))
                    } else {
                        data[indexOf] = it
                        adapter.notifyItemChanged(indexOf, it)
                    }
                }
            }
        },
        perfFrameRate = {
            Prefs.getS("pref.metrics.view.fps", "10").toInt()
        }
    )

    @SuppressLint("NotifyDataSetChanged")
    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                DATA_LOGGER_CONNECTED_EVENT -> {
                    renderingThread.start()
                }
                DATA_LOGGER_STOPPED_EVENT -> {
                    renderingThread.stop()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_metrics, container, false)
        val data = MetricsProvider().findMetrics(dataLoggerPreferences.getPIDsToQuery())
        val adapter = MetricsViewAdapter(root.context, data)

        dataLogger.observe(this){
            metricsCollector.append(it)
        }

        metricsCollector.applyFilter(dataLoggerPreferences.getPIDsToQuery())

        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(root.context, 1)
        recyclerView.adapter = adapter
        recyclerView.addOnItemTouchListener(
            ToggleToolbarDoubleClickListener(
                requireContext()
            )
        )

        if (dataLogger.isRunning()) {
            renderingThread.start()
        }

        return root
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.registerReceiver(receiver, IntentFilter().apply {
            addAction(DATA_LOGGER_CONNECTED_EVENT)
            addAction(DATA_LOGGER_STOPPED_EVENT)
        })
    }

    override fun onDetach() {
        super.onDetach()
        activity?.unregisterReceiver(receiver)
        renderingThread.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        renderingThread.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        renderingThread.stop()
    }
}