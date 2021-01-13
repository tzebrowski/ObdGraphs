package org.openobd2.core.logger.ui.dash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.openobd2.core.command.CommandReply
import org.openobd2.core.command.obd.ObdCommand
import org.openobd2.core.logger.Model
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.PidsRegistry

class DashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_dash, container, false)
        var data: MutableList<CommandReply<*>> = arrayListOf()

        val adapter = DashViewAdapter(root.context, data)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(root.context, 1)
        recyclerView.adapter = adapter

        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        var selectedPids = pref.getStringSet("pref.dash.pids.selected", emptySet())
        val pidRegistry = PidsRegistry.instance.genericRegistry

        selectedPids!!.forEach { s: String? ->
            data.add(CommandReply<Int>(ObdCommand(pidRegistry.findBy("01", s)), 0, ""))
        }

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