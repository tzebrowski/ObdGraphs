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
    private var data: MutableSet<CommandReply<*>> = mutableSetOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dashboardViewModel =
            ViewModelProvider(this).get(LiveDataViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_livedata, container, false)

        val adapter = LiveDataViewAdapter(root.context, data)

        Model.lveData.observe(viewLifecycleOwner, Observer {
            data.addAll(it.sortedBy {
                (it.command as ObdCommand).pid.order
            })
            adapter.notifyDataSetChanged()
        })
        val recyclerView: RecyclerView = root.findViewById(R.id.rrView)
        val numberOfColumns = 1
        recyclerView.layoutManager = GridLayoutManager(root.context, numberOfColumns)
        recyclerView.adapter = adapter
        return root
    }
}