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
import org.openobd2.core.logger.R
import org.openobd2.core.logger.bl.datalogger.DataLogger
import org.openobd2.core.logger.ui.common.convert
import org.openobd2.core.logger.ui.common.highLightText
import org.openobd2.core.logger.ui.common.isTablet
import org.openobd2.core.logger.ui.dashboard.round
import org.openobd2.core.logger.ui.graph.ValueScaler
import java.util.*

private const val LABEL_COLOR = "#01804F"

class GaugeViewAdapter internal constructor(
    private val context: Context,
    val data: MutableList<ObdMetric>,
    private val resourceId: Int
) : RecyclerView.Adapter<GaugeViewAdapter.ViewHolder>() {

    inner class ViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val label: TextView = itemView.findViewById(R.id.label)
        val value: TextView = itemView.findViewById(R.id.value)
        val avgValue: TextView? = itemView.findViewById(R.id.avg_value)
        val minValue: TextView = itemView.findViewById(R.id.min_value)
        val maxValue: TextView = itemView.findViewById(R.id.max_value)
        var commandRate: TextView? = itemView.findViewById(R.id.command_rate)
        var pidMode: TextView? = itemView.findViewById(R.id.pid_mode)
        var gauge: Gauge? = itemView.findViewById(R.id.gauge_view)
        var init: Boolean = false
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var view: View
    private val preferences: GaugePreferences by lazy { getGaugePreferences() }
    private val valueScaler = ValueScaler()

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
        if (!holder.init) {
            holder.label.text = metric.command.pid.description
            holder.pidMode?.run {
                text = "mode " + metric.command.pid.mode
                highLightText(
                    "mode", 0.4f,
                    Color.parseColor(LABEL_COLOR)
                )
            }

            view.post {
                if (isTablet(context)) {
                    val multiplier = calculateScaleMultiplier(view)
                    rescaleView(holder, multiplier)
                }
                holder.init = true
            }
        }

        holder.value.run {
            val units = (metric.command as ObdCommand).pid.units
            text = "${valueToString(metric)} $units"

            highLightText(
                units, 0.3f,
                Color.parseColor(LABEL_COLOR)
            )
        }

        DataLogger.instance.diagnostics().histogram().findBy(metric.command.pid).run {
            holder.minValue.run {
                text = "min\n ${metric.convert(min)}"
                highLightText(
                    "min", 0.5f,
                    Color.parseColor(LABEL_COLOR)
                )
            }

            holder.maxValue.run {
                text = "max\n  ${metric.convert(max)} "
                highLightText(
                    "max", 0.5f,
                    Color.parseColor(LABEL_COLOR)
                )
            }

            holder.avgValue?.run {
                text = "avg\n ${metric.convert(mean)}"
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
            startValue = (metric.command as ObdCommand).pid.min.toFloat()
            endValue = (metric.command as ObdCommand).pid.max.toFloat()
            value = metric.valueToLong().toFloat()
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    private fun calculateScaleMultiplier(view: View): Float {
        val width = view.measuredWidth.toFloat()
        val height = Resources.getSystem().displayMetrics.heightPixels / if (data.size > 2) 2 else 1

        val max =
            Resources.getSystem().displayMetrics.widthPixels * Resources.getSystem().displayMetrics.heightPixels.toFloat()
        val multiplier = valueScaler.scaleToNewRange(width * height, 0.0f, max, 1f, 3f)
        Log.v("GaugeViewAdapter", "r: $multiplier, w: $width,h: $height")
        return multiplier
    }

    private fun rescaleView(holder: ViewHolder, multiplier: Float) {

        Log.v("GaugeViewAdapter", "multiplier: $multiplier")

        holder.label.textSize *= multiplier * 0.75f
        holder.value.textSize *= multiplier * 0.85f
        holder.maxValue.textSize *= multiplier * 0.65f
        holder.minValue.textSize *= multiplier * 0.65f
        holder.avgValue?.let {
            it.textSize *= multiplier * 0.65f
        }
        holder.gauge?.let {
            it.strokeWidth *= multiplier * 1.15f
            it.init()
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

    private fun valueToString(metric: ObdMetric): String? {
        return if (metric.value == null) {
            "No data"
        } else {
            return metric.convert(metric.valueToDouble()).toString()
        }
    }
}