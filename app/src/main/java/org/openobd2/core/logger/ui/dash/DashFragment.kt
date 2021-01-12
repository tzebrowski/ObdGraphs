package org.openobd2.core.logger.ui.dash

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

class DashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_dash, container, false)
        var data: MutableList<CommandReply<*>> = arrayListOf()

//        Thread.currentThread().contextClassLoader
//            .getResourceAsStream("generic.json").use { source ->
//                val registry = PidRegistry.builder().source(source).build()
//                data.add( CommandReply<Int>(ObdCommand(registry.findBy("01","0C")),5000,""))
//                data.add( CommandReply<Int>(ObdCommand(registry.findBy("01","05")),80,""))
//                data.add( CommandReply<Int>(ObdCommand(registry.findBy("01","0D")),100,""))
//            }

        val adapter = DashViewAdapter(root.context, data)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(root.context, 1)
        recyclerView.adapter = adapter

        Model.liveData.observe(viewLifecycleOwner, Observer {
            val sortedList = it.sortedBy {
                (it.command as ObdCommand).pid.order
            }
            data.clear()
            data.addAll(sortedList)
            adapter.notifyDataSetChanged()
        })


        return root
    }
}