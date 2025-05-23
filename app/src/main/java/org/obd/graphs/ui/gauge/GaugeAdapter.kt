 /**
 * Copyright 2019-2025, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.ui.gauge

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.PREF_ALERTING_ENABLED
import org.obd.graphs.bl.collector.Metric
import org.obd.graphs.R
import org.obd.graphs.ValueConverter
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.format
import org.obd.graphs.toFloat
import org.obd.graphs.modules
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.round
import org.obd.graphs.ui.common.*
import org.obd.graphs.ui.recycler.RecyclerViewAdapter
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.diagnostic.RateType
import kotlin.math.roundToInt

class GaugeAdapter(
    context: Context,
    data: MutableList<Metric>,
    resourceId: Int,
    height: Int? = null
) :
    RecyclerViewAdapter<GaugeAdapter.ViewHolder>(context, data, resourceId, height) {

    inner class ViewHolder internal constructor(itemView: View):
        RecyclerView.ViewHolder(itemView) {

        val label: TextView = itemView.findViewById(R.id.label)
        val value: TextView = itemView.findViewById(R.id.value)
        val avgValue: TextView? = itemView.findViewById(R.id.avg_value)
        val minValue: TextView = itemView.findViewById(R.id.min_value)
        val maxValue: TextView = itemView.findViewById(R.id.max_value)
        var commandRate: TextView? = itemView.findViewById(R.id.command_rate)
        var resourceFile: TextView? = itemView.findViewById(R.id.resource_file)
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
                var multiplier = calculateScaleMultiplier(itemView) * 0.29f
                multiplier *= if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1f else {
                    when (data.size) {
                        1 -> 0.9f
                        3 -> 1.4f
                        2 -> 1.4f
                        else -> 1f
                    }
                }
                rescaleTextSize(this, multiplier)
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
    private val valueConverter = ValueConverter()

    override fun getItemId(position: Int): Long {
        return data[position].source.command.pid.id
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
        val pid = metric.pid()

        if (!holder.init) {
            holder.label.text = pid.longDescription ?: pid.description
            holder.resourceFile?.run {
                val resourceFile = modules.getDefaultModules()[pid.resourceFile]
                    ?: pid.resourceFile
                text = resourceFile
                highLightText(
                    resourceFile, 0.5f,
                    Color.WHITE
                )
            }

            holder.avgValue?.isGone = !pid.historgam.isAvgEnabled
            holder.minValue.isGone = !pid.historgam.isMinEnabled
            holder.maxValue.isGone = !pid.historgam.isMaxEnabled
            holder.init = true
        }

        holder.value.run {
            val units = (metric.source.command as ObdCommand).pid.units?:""
            val txt = "${metric.source.format(castToInt = false)} $units"
            text = txt
            setTextColor(if (inAlertState(metric)) {
                Color.RED
            } else {
                Color.WHITE
            })

            highLightText(
                units, 0.3f,
                COLOR_PHILIPPINE_GREEN
            )

        }

        if (pid.historgam.isMinEnabled) {
            holder.minValue.run {
                val txt = "min\n ${metric.min.format(pid)}"
                text = txt
                highLightText(
                    "min", 0.5f,
                    COLOR_PHILIPPINE_GREEN
                )
            }
        }


        if (pid.historgam.isMaxEnabled) {
            holder.maxValue.run {
                val txt = "max\n  ${metric.max.format(pid)}"
                text = txt
                highLightText(
                    "max", 0.5f,
                    COLOR_PHILIPPINE_GREEN
                )
            }
        }

        if (pid.historgam.isAvgEnabled) {
            holder.avgValue?.run {
                val txt = "avg\n ${metric.mean.format(pid)}"
                text = txt
                highLightText(
                    "avg", 0.5f,
                    COLOR_PHILIPPINE_GREEN
                )
            }
        }

        holder.commandRate?.run {
            if (preferences.commandRateEnabled) {
                this.visibility = View.VISIBLE
                val rate = dataLogger.getDiagnostics().rate()
                    .findBy(RateType.MEAN, pid)
                val txt = "rate ${rate.get().value.round(2)}"
                text = txt
                highLightText(
                    "rate", 0.4f,
                    COLOR_PHILIPPINE_GREEN
                )
            } else {
                this.visibility = View.INVISIBLE
            }
        }

        if (holder.gauge == null) {
            holder.gauge = holder.itemView.findViewById(R.id.gauge_view)
        }

        holder.gauge?.apply {
            (metric.source.command as ObdCommand).pid?.let {
                if (it.min != null) {
                    startValue = it.min.toFloat()
                }
                if (it.max != null) {
                    endValue = it.max.toFloat()
                }
            }
            value = metric.source.toFloat()
            invalidate()
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

        return valueConverter.scaleToNewRange(currentSize, 0.0f, targetSize, 1f, 3f)
    }

    private fun rescaleTextSize(holder: ViewHolder, multiplier: Float) {
        holder.label.textSize *= multiplier * 0.60f
        holder.value.textSize *= multiplier * 0.60f
        holder.maxValue.textSize *= multiplier * 0.45f
        holder.minValue.textSize *= multiplier * 0.45f
        holder.avgValue?.let {
            it.textSize *= multiplier * 0.45f
        }
    }

    private fun updateHeight(parent: ViewGroup) {
        if (height == null) {
            if (isTablet()) {
                view.layoutParams.height =
                    Resources.getSystem().displayMetrics.heightPixels / if (data.size > 2) 2 else 1
            } else {
                val x =
                    if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1 else {
                        when (data.size) {
                            1 -> 1
                            2 -> 2
                            else -> 3
                        }
                    }

                view.layoutParams.height = parent.measuredHeight / x
            }
        } else {
            view.layoutParams.height = height
        }
    }

    private fun inAlertState(metric: Metric) =  Prefs.getBoolean(PREF_ALERTING_ENABLED, false) &&
            (metric.source.isUpperAlert || metric.source.isLowerAlert)
}
