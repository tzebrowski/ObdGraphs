package org.openobd2.core.logger.ui.dash

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.command.obd.ObdCommand
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.DataLoggerService
import org.openobd2.core.logger.bl.ModelChangePublisher
import org.openobd2.core.logger.ui.preferences.Preferences

class DashFragment : Fragment() {

    lateinit var root: View

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        root?.let {
            val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
            val spanCount =
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1
            recyclerView.layoutManager = GridLayoutManager(root.context, spanCount)
            recyclerView.refreshDrawableState()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val selectedPids = Preferences.getStringSet(requireContext(), "pref.dash.pids.selected")
        val data = DataLoggerService.dataLogger.buildMetricsBy(selectedPids)

        root = inflater.inflate(R.layout.fragment_dash, container, false)
        val adapter = DashViewAdapter(root.context, data)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        val spanCount =
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1

        recyclerView.layoutManager = GridLayoutManager(root.context, spanCount)
        recyclerView.adapter = adapter
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

        return root
    }
}