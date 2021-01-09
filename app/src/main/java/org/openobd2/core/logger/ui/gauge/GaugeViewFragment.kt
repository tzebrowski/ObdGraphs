package org.openobd2.core.logger.ui.gauge

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

class GaugeViewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_gauge, container, false)

        var data: MutableList<CommandReply<*>> = arrayListOf()
        val adapter = GaugeViewAdapter(root.context, data)

        Model.liveData.observe(viewLifecycleOwner, Observer {
            val sortedList = it.sortedBy {
                (it.command as ObdCommand).pid.order
            }
            data.clear()
            data.addAll(sortedList)
            adapter.notifyDataSetChanged()
        })

        val recyclerView: RecyclerView = root.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(root.context, 2)
        recyclerView.adapter = adapter
        return root
    }
}