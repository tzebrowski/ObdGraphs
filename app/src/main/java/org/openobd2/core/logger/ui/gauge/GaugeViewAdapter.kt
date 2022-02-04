package org.openobd2.core.logger.ui.gauge


import android.content.Context
import android.graphics.Color
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
import org.openobd2.core.logger.bl.DataLogger
import org.openobd2.core.logger.ui.common.SpannableStringUtils
import org.openobd2.core.logger.ui.dashboard.round
import org.openobd2.core.logger.ui.preferences.Prefs
import org.openobd2.core.logger.ui.preferences.isEnabled
import pl.pawelkleczkowski.customgauge.CustomGauge
import java.util.Collections

private val DEFAULT_HEIGHT = 230
private val LABEL_COLOR = "#01804F"
private val COMMANDS_RATE_PREF_KEY = "pref.adapter.diagnosis.command.frequency.enabled"

class GaugeViewAdapter internal constructor(
    context: Context,
    val data: MutableList<ObdMetric>,
    private val resourceId: Int
) : RecyclerView.Adapter<GaugeViewAdapter.ViewHolder>() {

    inner class ViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var label: TextView = itemView.findViewById(R.id.label)
        var value: TextView = itemView.findViewById(R.id.value)
        var avgValue: TextView? = itemView.findViewById(R.id.avg_value)
        var minValue: TextView = itemView.findViewById(R.id.min_value)
        var maxValue: TextView = itemView.findViewById(R.id.max_value)
        var commandRate: TextView? = itemView.findViewById(R.id.command_rate)
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var view: View

    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(data, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        view = inflater.inflate(resourceId, parent, false)

        (parent.measuredHeight / 1.8).run {
            if (this > 0) {
                if (data.size > 2)
                    view.layoutParams.height = this.toInt()
            } else {
                view.layoutParams.height = DEFAULT_HEIGHT
            }
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val metric = data.elementAt(position)
        holder.label.text = metric.command.label

        holder.value.run {
            val units = (metric.command as ObdCommand).pid.units
            text = metric.valueToString() + " " + units
            SpannableStringUtils.setHighLightedText(
                this, units, 0.3f,
                Color.parseColor(LABEL_COLOR)
            )
        }

        DataLogger.INSTANCE.diagnostics().findHistogramBy(metric.command.pid).run{
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

            holder.commandRate?.run {
                if (Prefs.isEnabled(COMMANDS_RATE_PREF_KEY)){
                    this.visibility = View.VISIBLE
                    val rate = DataLogger.INSTANCE.diagnostics().getRateBy(RateType.MEAN, metric.command.pid)
                    text = "rate " + rate.get().value.round(2)
                    SpannableStringUtils.setHighLightedText(
                        this, "rate", 0.4f,
                        Color.parseColor(LABEL_COLOR)
                    )
                } else {
                    this.visibility = View.INVISIBLE
                }
            }

            (holder.itemView.findViewById(R.id.gauge_view) as CustomGauge?)?.apply {
                startValue = (metric.command as ObdCommand).pid.min.toInt()
                endValue = (metric.command as ObdCommand).pid.max.toInt()
                value = metric.valueToLong().toInt()
            }
        }
    }

    private fun convert(
        metric: ObdMetric,
        value: Double
    ): Number {
         if (metric.command.pid.type == null){
             return value.round(2)
         }
        return when (metric.command.pid.type) {
            PidDefinition.ValueType.DOUBLE -> value.round(2)
            PidDefinition.ValueType.INT -> value.toInt()
            PidDefinition.ValueType.SHORT -> value.toInt()
            else -> value.round(1)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}