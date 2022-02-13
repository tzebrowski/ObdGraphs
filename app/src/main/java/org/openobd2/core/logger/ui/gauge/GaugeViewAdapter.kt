package org.openobd2.core.logger.ui.gauge


import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.metrics.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.diagnostic.RateType
import org.obd.metrics.pid.PidDefinition
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.datalogger.DataLogger
import org.openobd2.core.logger.ui.common.SpannableStringUtils
import org.openobd2.core.logger.ui.common.isTablet
import org.openobd2.core.logger.ui.dashboard.round
import pl.pawelkleczkowski.customgauge.CustomGauge
import java.util.*

private val DEFAULT_HEIGHT = 230
private val LABEL_COLOR = "#01804F"

class GaugeViewAdapter internal constructor(
    private val context: Context,
    val data: MutableList<ObdMetric>,
    private val resourceId: Int
): RecyclerView.Adapter<GaugeViewAdapter.ViewHolder>() {

    inner class ViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val label: TextView = itemView.findViewById(R.id.label)
        val value: TextView = itemView.findViewById(R.id.value)
        val avgValue: TextView? = itemView.findViewById(R.id.avg_value)
        val minValue: TextView = itemView.findViewById(R.id.min_value)
        val maxValue: TextView = itemView.findViewById(R.id.max_value)
        var commandRate: TextView? = itemView.findViewById(R.id.command_rate)
        var gauge: CustomGauge? = itemView.findViewById(R.id.gauge_view)
        var init: Boolean = false
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var view: View
    private val preferences: GaugePreferences by lazy { getGaugePreferences() }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(data, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        view = inflater.inflate(resourceId, parent, false)
        if (isTablet(context)) {
            (parent.measuredHeight / 1.8).run {
                if (this > 0) {
                    if (data.size > 2)
                        view.layoutParams.height = this.toInt()
                } else {
                    view.layoutParams.height = DEFAULT_HEIGHT
                }
            }
        } else{
            view.layoutParams.height = parent.measuredHeight  / 3
        }
        return ViewHolder(view)
    }

    var currentTs:Long = 0

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val metric = data.elementAt(position)

        logTsDiff(metric)

        if (!holder.init){
            holder.label.text = metric.command.label
            holder.init = true
            when (data.size){
                1 -> rescaleView(holder,1.4f,1.2f)
                2 -> rescaleView(holder,1.2f,1.1f)
                3 -> rescaleView(holder,1.1f,1.1f)
                4 -> rescaleView(holder,1.1f,1.1f)
            }
        }

        holder.value.run {
            val units = (metric.command as ObdCommand).pid.units
            text = metric.valueToString() + " " + units

            SpannableStringUtils.setHighLightedText(
                this, units, 0.3f,
                Color.parseColor(LABEL_COLOR))
        }

        DataLogger.INSTANCE.diagnostics().histogram().findBy(metric.command.pid).run {
            holder.minValue.run {
                text = "min\n ${convert(metric, min)}"
                SpannableStringUtils.setHighLightedText(
                    this, "min", 0.5f,
                    Color.parseColor(LABEL_COLOR)
                )
            }

            holder.maxValue.run {
                text = "max\n  ${convert(metric, max)} "
                SpannableStringUtils.setHighLightedText(
                    this, "max", 0.5f,
                    Color.parseColor(LABEL_COLOR)
                )
            }

            holder.avgValue?.run {
                text = "avg\n ${convert(metric, mean)}"
                SpannableStringUtils.setHighLightedText(
                    this, "avg", 0.5f,
                    Color.parseColor(LABEL_COLOR)
                )
            }
        }
        holder.commandRate?.run {
            if (preferences.commandRateEnabled) {
                this.visibility = View.VISIBLE
                val rate = DataLogger.INSTANCE.diagnostics().rate()
                    .findBy(RateType.MEAN, metric.command.pid)
                text = "rate " + rate.get().value.round(2)
                SpannableStringUtils.setHighLightedText(
                    this, "rate", 0.4f,
                    Color.parseColor(LABEL_COLOR)
                )
            } else {
                this.visibility = View.INVISIBLE
            }

        }
        if (holder.gauge == null) {
            holder.gauge = holder.itemView.findViewById(R.id.gauge_view)
        }

        holder.gauge?.apply {
            startValue = (metric.command as ObdCommand).pid.min.toInt()
            endValue = (metric.command as ObdCommand).pid.max.toInt()
            value = metric.valueToLong().toInt()
        }

    }

    private fun rescaleView(holder: ViewHolder, scale1: Float, scale2: Float) {
        holder.label.textSize = (holder.label.textSize * scale1)

        holder.value.let {
            it.textSize = (it.textSize * scale1)
        }

        holder.maxValue.let {
            it.textSize = (it.textSize * scale2)
        }

        holder.minValue.let {
            it.textSize = (it.textSize * scale2)
        }

        holder.avgValue?.let {
            it.textSize = (it.textSize * scale2)
        }
    }

    private fun logTsDiff(metric: ObdMetric) {
        currentTs = if (currentTs.equals(0)) {
            metric.timestamp
        } else {
            val diff = metric.timestamp - currentTs
            Log.v("LogTS", "${metric.command.pid.description} = ${diff}")
            metric.timestamp
        }
    }

    private fun convert(metric: ObdMetric, value: Double): Number {

        if (value.isNaN()){
            return 0.0
        }

        return if (metric.command.pid.type == null) 0.0 else
            metric.command.pid.type.let {
                return when (metric.command.pid.type) {
                    PidDefinition.ValueType.DOUBLE -> value.round(2)
                    PidDefinition.ValueType.INT -> value.toInt()
                    PidDefinition.ValueType.SHORT -> value.toInt()
                    else -> value.round(1)
                }
            }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}