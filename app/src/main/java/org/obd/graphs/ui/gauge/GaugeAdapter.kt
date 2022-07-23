package org.obd.graphs.ui.gauge

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLogger
import org.obd.graphs.ui.recycler.SimpleAdapter
import org.obd.graphs.ui.common.convert
import org.obd.graphs.ui.common.highLightText
import org.obd.graphs.ui.common.isTablet
import org.obd.graphs.ui.dashboard.round
import org.obd.graphs.ui.graph.ValueScaler
import org.obd.graphs.ui.preferences.Prefs
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.diagnostic.RateType
import kotlin.math.roundToInt

private const val LABEL_COLOR = "#01804F"

class GaugeAdapter(
    context: Context,
    data: MutableList<ObdMetric>,
    resourceId: Int,
    height: Int? = null
) :
    SimpleAdapter<GaugeAdapter.ViewHolder>(context, data, resourceId, height) {

    inner class ViewHolder internal constructor(itemView: View):
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

        init {

            gauge?.gaugeDrawScale = Prefs.getBoolean("pref.gauge_display_scale", true)
            val displayBackground = Prefs.getBoolean("pref.gauge_display_background", true)

            if (displayBackground) {
                updateDrawable()
            } else {
                view.background = null
            }

            if (isTablet()) {
                rescaleTextSize(this, calculateScaleMultiplier(itemView))
            } else {
                rescaleTextSize(this, calculateScaleMultiplier(itemView) * 0.29f)
            }
        }

        private fun updateDrawable() {
            itemView.findViewById<ImageView>(R.id.gauge_background).run {
                val filter: ColorFilter =
                    PorterDuffColorFilter(
                        Prefs.getInt("pref.gauge_background_color", -1),
                        PorterDuff.Mode.SRC_IN
                    )
                drawable.colorFilter = filter
            }
        }
    }

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private lateinit var view: View
    private val preferences: GaugePreferences by lazy { getGaugePreferences() }
    private val valueScaler = ValueScaler()

    override fun getItemId(position: Int): Long {
        return data[position].command.pid.id
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
                val txt = "mode ${metric.command.pid.mode}"
                text = txt
                highLightText(
                    "mode", 0.4f,
                    Color.parseColor(LABEL_COLOR)
                )
            }
            holder.init = true
        }

        holder.value.run {
            val units = (metric.command as ObdCommand).pid.units
            val txt = "${valueToString(metric)} $units"
            text = txt

            highLightText(
                units, 0.3f,
                Color.parseColor(LABEL_COLOR)
            )
        }

        DataLogger.instance.diagnostics().histogram().findBy(metric.command.pid).run {
            holder.minValue.run {
                val txt = "min\n ${metric.convert(min)}"
                text = txt
                highLightText(
                    "min", 0.5f,
                    Color.parseColor(LABEL_COLOR)
                )
            }

            holder.maxValue.run {
                val txt = "max\n  ${metric.convert(max)} "
                text = txt
                highLightText(
                    "max", 0.5f,
                    Color.parseColor(LABEL_COLOR)
                )
            }

            holder.avgValue?.run {
                val txt = "avg\n ${metric.convert(mean)}"
                text = txt
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
                val txt = "rate ${rate.get().value.round(2)}"
                text = txt
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

    private fun calculateScaleMultiplier(itemView: View): Float {
        itemView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        val widthDivider = when (data.size) {
            1 -> 1
            2 -> 2
            else -> {
                (data.size / 2f).roundToInt()
            }
        }

        val width = Resources.getSystem().displayMetrics.widthPixels / widthDivider
        val height = itemView.measuredHeight.toFloat()

        val targetSize =
            Resources.getSystem().displayMetrics.widthPixels * Resources.getSystem().displayMetrics.heightPixels.toFloat()
        val currentSize = width * height
        return valueScaler.scaleToNewRange(currentSize, 0.0f, targetSize, 1f, 3f)
    }

    private fun rescaleTextSize(holder: ViewHolder, multiplier: Float) {
        holder.label.textSize *= multiplier * 0.75f
        holder.value.textSize *= multiplier * 0.85f
        holder.maxValue.textSize *= multiplier * 0.65f
        holder.minValue.textSize *= multiplier * 0.65f
        holder.avgValue?.let {
            it.textSize *= multiplier * 0.65f
        }
    }

    private fun updateHeight(parent: ViewGroup) {
        if (height == null) {
            if (isTablet()) {
                view.layoutParams.height =
                    Resources.getSystem().displayMetrics.heightPixels / if (data.size > 2) 2 else 1
            } else {
                val x =
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else 3
                view.layoutParams.height = parent.measuredHeight / x
            }
        } else {
            view.layoutParams.height = height
        }
    }

    private fun valueToString(metric: ObdMetric): String {
        return if (metric.value == null) {
            "No data"
        } else {
            return metric.convert(metric.valueToDouble()).toString()
        }
    }
}