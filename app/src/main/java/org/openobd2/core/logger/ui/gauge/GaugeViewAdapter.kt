package org.openobd2.core.logger.ui.gauge


import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
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
import org.openobd2.core.logger.ui.common.highLightText
import org.openobd2.core.logger.ui.common.isTablet
import org.openobd2.core.logger.ui.dashboard.round
import org.openobd2.core.logger.ui.graph.ValueScaler
import pl.pawelkleczkowski.customgauge.CustomGauge
import java.util.*

private const val LABEL_COLOR = "#01804F"

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
    private val valueScaler  = ValueScaler()

    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(data, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        view = inflater.inflate(resourceId, parent, false)
        updateHeight(parent)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val metric = data.elementAt(position)

        if (!holder.init){
            holder.label.text = metric.command.label
            view.post {
                if (isTablet(context)) {
                    val multiplier = calculateScaleMultiplier(view)
                    rescaleView(holder,multiplier)
                }
                holder.init = true
            }
        }

        holder.value.run {
            val units = (metric.command as ObdCommand).pid.units
            text = metric.valueToString() + " " + units

            highLightText(
                units, 0.3f,
                Color.parseColor(LABEL_COLOR))
        }

        DataLogger.instance.diagnostics().histogram().findBy(metric.command.pid).run {
            holder.minValue.run {
                text = "min\n ${convert(metric, min)}"
                highLightText(
                    "min", 0.5f,
                    Color.parseColor(LABEL_COLOR)
                )
            }

            holder.maxValue.run {
                text = "max\n  ${convert(metric, max)} "
                highLightText(
                    "max", 0.5f,
                    Color.parseColor(LABEL_COLOR)
                )
            }

            holder.avgValue?.run {
                text = "avg\n ${convert(metric, mean)}"
                highLightText(
                    "avg", 0.5f,
                    Color.parseColor(LABEL_COLOR)
                )
            }
        }
        holder.commandRate?.run {
            if (preferences.commandRateEnabled) {
                this.visibility = View.VISIBLE
                val rate = DataLogger.instance.diagnostics().rate()
                    .findBy(RateType.MEAN, metric.command.pid)
                text = "rate " + rate.get().value.round(2)
                highLightText(
                    "rate", 0.4f,
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

    override fun getItemCount(): Int {
        return data.size
    }

    private fun calculateScaleMultiplier(view: View): Float {
        val width = view.measuredWidth.toFloat()
        val height = Resources.getSystem().displayMetrics.heightPixels / if (data.size > 2) 2 else 1

        val max = Resources.getSystem().displayMetrics.widthPixels * Resources.getSystem().displayMetrics.heightPixels
        val currentVal = width * height

        val ratio = valueScaler.scaleToNewRange(currentVal, 0.0f, max.toFloat(), 0.70f, 2.5f)
        Log.e("GaugeViewAdapter", "r: $ratio, w: $width,h: $height")
        return ratio
    }

    private fun rescaleView(holder: ViewHolder, multiplier: Float) {
        holder.label.textSize = (holder.label.textSize * multiplier)
        holder.value.textSize *= multiplier
        holder.maxValue.textSize *= multiplier * 0.85f
        holder.minValue.textSize *= multiplier * 0.85f
        holder.avgValue?.let {
            it.textSize *= multiplier * 0.85f
        }
    }

    private fun convert(metric: ObdMetric, value: Double): Number {
        if (value.isNaN()){
            return 0.0
        }
        return if (metric.command.pid.type == null) value.round(2) else
            metric.command.pid.type.let {
                return when (metric.command.pid.type) {
                    PidDefinition.ValueType.DOUBLE -> value.round(2)
                    PidDefinition.ValueType.INT -> value.toInt()
                    PidDefinition.ValueType.SHORT -> value.toInt()
                    else -> value.round(1)
                }
            }
    }

    private fun updateHeight(parent: ViewGroup) {
        if (isTablet(context)) {
            val heightPixels = Resources.getSystem().displayMetrics.heightPixels
            view.layoutParams.height = heightPixels / if (data.size > 2) 2 else 1
        } else {
            val x =
                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 3
            view.layoutParams.height = parent.measuredHeight / x
        }
    }
}