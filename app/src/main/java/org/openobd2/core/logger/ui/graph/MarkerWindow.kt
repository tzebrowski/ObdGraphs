package org.openobd2.core.logger.ui.graph

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import org.obd.metrics.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinitionRegistry
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.datalogger.DataLogger

class MarkerWindow(context: Context?, layoutResource: Int) :
    MarkerView(context, layoutResource) {
    private val valueScaler  = ValueScaler()

    private fun buildMetrics(value: Set<Pair<Long,Float>>): MutableList<ObdMetric> {
        val pidRegistry: PidDefinitionRegistry =  DataLogger.instance.pidDefinitionRegistry()
        return value.map {
            val pid = pidRegistry.findBy(it.first)
            val value  = valueScaler.scaleToPidRange(pid, it.second)
            ObdMetric.builder().command(ObdCommand(pid)).value(value).build()
        }.toMutableList()
    }

    override fun refreshContent(e: Entry, highlight: Highlight?) {
        val id = (e.data as Long)
        val emptyMetrics = buildMetrics(setOf(Pair(id,e.y)))
        val adapter = MarkerWindowViewAdapter(context, emptyMetrics)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.adapter = adapter
        super.refreshContent(e, highlight)
    }
}