package org.openobd2.core.logger.ui.gauge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.openobd2.core.command.CommandReply
import org.openobd2.core.command.obd.ObdCommand
import org.openobd2.core.logger.Model
import org.openobd2.core.logger.R
import org.openobd2.core.logger.SelectedPids

class GaugeViewFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        var (selectedPids, data: MutableList<CommandReply<*>>) = SelectedPids.get(requireContext(), "pref.gauge.pids.selected")

        val root = inflater.inflate(R.layout.fragment_gauge, container, false)
        val adapter = GaugeViewAdapter(root.context, data)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(root.context, 2)
        recyclerView.adapter = adapter

        Model.liveData.observe(viewLifecycleOwner, Observer {
            val filter =
                    it.filter { commandReply -> selectedPids.contains((commandReply.command as ObdCommand).pid.pid) }
            data.clear()
            data.addAll(filter)
            adapter.notifyDataSetChanged()
        })

        return root
    }


}