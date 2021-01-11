package org.openobd2.core.logger.ui.dash

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.Legend.LegendForm
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.components.YAxis.YAxisLabelPosition
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import org.openobd2.core.command.CommandReply
import org.openobd2.core.command.obd.ObdCommand
import org.openobd2.core.logger.R
import org.openobd2.core.logger.ui.gauge.GaugeViewAdapter
import org.openobd2.core.pid.PidDefinition
import org.openobd2.core.pid.PidRegistry


class DashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_dash, container, false)
        var data: MutableList<CommandReply<*>> = arrayListOf()

        Thread.currentThread().contextClassLoader
            .getResourceAsStream("generic.json").use { source ->
                val registry = PidRegistry.builder().source(source).build()
                data.add( CommandReply<Int>(ObdCommand(registry.findBy("01","0C")),5000,""))
                data.add( CommandReply<Int>(ObdCommand(registry.findBy("01","05")),80,""))
                data.add( CommandReply<Int>(ObdCommand(registry.findBy("01","0D")),100,""))
            }

        val adapter = DashViewAdapter(root.context, data)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(root.context, 1)
        recyclerView.adapter = adapter
        return root
    }
}