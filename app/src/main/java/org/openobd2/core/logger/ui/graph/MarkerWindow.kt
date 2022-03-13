package org.openobd2.core.logger.ui.graph

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import org.obd.metrics.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinitionRegistry
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.datalogger.DataLogger

class MarkerWindow(context: Context?, layoutResource: Int, private val chart: LineChart) :
    MarkerView(context, layoutResource) {
    private val valueScaler  = ValueScaler()

    private fun buildMetrics( id: Long, v: Float): ObdMetric {
        val pidRegistry: PidDefinitionRegistry =  DataLogger.instance.pidDefinitionRegistry()
        val pid = pidRegistry.findBy(id)
        val value  = valueScaler.scaleToPidRange(pid,v)
        return ObdMetric.builder().command(ObdCommand(pid)).value(value).build()
    }

    override fun refreshContent(e: Entry, highlight: Highlight?) {
        val metrics = findClosestMetrics(e)

        val adapter = MarkerWindowViewAdapter(context, metrics)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        recyclerView.adapter = adapter
        super.refreshContent(e, highlight)
    }

    private fun findClosestMetrics(e: Entry): MutableCollection<ObdMetric> {
        val metricsMap = mutableMapOf<Long, ObdMetric>()
        e.data?.let {
            metricsMap[(e.data.toString().toLong())] =
                buildMetrics((e.data.toString().toLong() as Long), e.y)
        }

        chart.data.dataSets.forEach {
            var x = 0

            do {
                val entriesForXValue = it.getEntriesForXValue(e.x + x)
                if (entriesForXValue.isNotEmpty()) {
                    entriesForXValue.forEach { entry ->
                        e.data?.let {
                            val id = entry.data.toString().toLong()
                            metricsMap[id] =
                                buildMetrics(id, entry.y)
                        }
                    }
                } else {
                    x = updateXValue(x)
                }

            } while (entriesForXValue.isEmpty())
        }
        return metricsMap.values
    }

    private fun updateXValue(x: Int): Int {
        var x1 = x
        when {
            x1 == 0 -> {
                x1++
            }
            x1 < 0 -> {
                x1 *= -1
                x1++
            }
            else -> {
                x1 *= -1
            }
        }
        return x1
    }
}