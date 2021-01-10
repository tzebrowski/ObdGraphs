package org.openobd2.core.logger.ui.livedata

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.openobd2.core.command.CommandReply
import org.openobd2.core.command.obd.ObdCommand
import org.openobd2.core.logger.Model
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.DataLoggerService


class LiveDataFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_livedata, container, false)

        var data: MutableList<CommandReply<*>> = arrayListOf()
        val adapter = LiveDataViewAdapter(root.context, data)

        Model.liveData.observe(viewLifecycleOwner, Observer {
            val sortedList = it.sortedBy {
                (it.command as ObdCommand).pid.order
            }
            data.clear()
            data.addAll(sortedList)
            adapter.notifyDataSetChanged()
        })

        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(root.context, 1)
        recyclerView.adapter = adapter

        val btnStop: FloatingActionButton = root.findViewById(R.id.btn_stop)
        btnStop.setOnClickListener(View.OnClickListener {
            Log.i("DATA_LOGGER_UI", "Stop data logging ")
            DataLoggerService.stopAction(this.requireContext())
        })

        val btnStart: FloatingActionButton = root.findViewById(R.id.btn_start)
        btnStart.setOnClickListener(View.OnClickListener {
            Log.i("DATA_LOGGER_UI", "Start data logging")
            DataLoggerService.startAction(this.requireContext())
        })
        return root
    }
}