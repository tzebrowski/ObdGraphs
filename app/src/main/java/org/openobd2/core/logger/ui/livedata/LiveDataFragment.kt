package org.openobd2.core.logger.ui.livedata

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.openobd2.core.command.CommandReply
import org.openobd2.core.command.obd.ObdCommand
import org.openobd2.core.logger.Model
import org.openobd2.core.logger.R


class LiveDataFragment : Fragment() {

    private lateinit var dashboardViewModel: LiveDataViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dashboardViewModel =
            ViewModelProvider(this).get(LiveDataViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_livedata, container, false)

        var data: MutableList<CommandReply<*>> = arrayListOf()
        val adapter = LiveDataViewAdapter(root.context, data)

        Model.lveData.observe(viewLifecycleOwner, Observer {
            val sortedList = it.sortedBy {
                (it.command as ObdCommand).pid.order
            }
            data.clear()
            data.addAll(sortedList)
            adapter.notifyDataSetChanged()
        })

        val recyclerView: RecyclerView = root.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(root.context, 1)
        recyclerView.adapter = adapter
        return root
    }
}