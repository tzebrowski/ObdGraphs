package org.obd.graphs.ui.graph

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import org.obd.graphs.R
import org.obd.graphs.ValueScaler
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinitionRegistry

private const val SEARCH_SCOPE = 300
private const val LOG_KEY = "MarkerWindow"

class MarkerWindow(context: Context?, layoutResource: Int, private val chart: LineChart) :
    MarkerView(context, layoutResource) {
    private val valueScaler = ValueScaler()

    private fun buildMetrics(id: Long, v: Float): ObdMetric {
        val pidRegistry: PidDefinitionRegistry = dataLogger.pidDefinitionRegistry()
        val pid = pidRegistry.findBy(id)
        val value = valueScaler.scaleToPidRange(pid, v)
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
                buildMetrics((e.data.toString().toLong()), e.y)
        }

        var time = System.currentTimeMillis()
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
                    if (x > SEARCH_SCOPE) {
                        Log.d(LOG_KEY, "Did not find entry for=${it.label}.")
                        break
                    }
                    x = updateXValue(x)
                }

            } while (entriesForXValue.isEmpty())
        }
        time = System.currentTimeMillis() - time
        Log.d(LOG_KEY, "Build map, time: ${time}ms , values: ${metricsMap.values}.")
        return metricsMap.values.sortedBy { i -> i.command.pid.description }.toMutableList()
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