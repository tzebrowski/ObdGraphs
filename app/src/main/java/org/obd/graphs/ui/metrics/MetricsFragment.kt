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
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTING_EVENT
import org.obd.graphs.bl.datalogger.MetricsProvider
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.ui.common.ToggleToolbarDoubleClickListener

class MetricsFragment : Fragment() {
    lateinit var adapter: MetricsViewAdapter

    @SuppressLint("NotifyDataSetChanged")
    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            adapter.let {
                it.data.clear()
                it.data.addAll(MetricsProvider().findMetrics(dataLoggerPreferences.getPIDsToQuery()))
                it.notifyDataSetChanged()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_metrics, container, false)
        val data = MetricsProvider().findMetrics(dataLoggerPreferences.getPIDsToQuery())
        adapter = MetricsViewAdapter(root.context, data)

        dataLogger.observe(this){
            val indexOf = data.indexOf(it)
            if (indexOf == -1) {
                data.add(it)
                adapter.notifyItemInserted(data.indexOf(it))
            } else {
                data[indexOf] = it
                adapter.notifyItemChanged(indexOf, it)
            }
        }

        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(root.context, 1)
        recyclerView.adapter = adapter
        recyclerView.addOnItemTouchListener(
            ToggleToolbarDoubleClickListener(
                requireContext()
            )
        )
        return root
    }



    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.registerReceiver(receiver, IntentFilter().apply {
            addAction(DATA_LOGGER_CONNECTING_EVENT)
        })
    }

    override fun onDetach() {
        super.onDetach()
        activity?.unregisterReceiver(receiver)
    }
}