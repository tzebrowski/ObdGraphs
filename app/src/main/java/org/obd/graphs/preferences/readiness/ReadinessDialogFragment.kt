/*
 * Copyright 2019-2026, Tomasz Żebrowski
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
package org.obd.graphs.preferences.readiness

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.bl.readiness.PREF_READINESS_LAST
import org.obd.graphs.bl.readiness.READINESS_COUNTER_PIDS
import org.obd.graphs.bl.readiness.ReadinessCounter
import org.obd.graphs.bl.readiness.ReadinessState
import org.obd.graphs.bl.readiness.ReadinessView
import org.obd.graphs.bl.readiness.shownMonitors
import org.obd.graphs.bl.readiness.snapshot
import org.obd.graphs.bl.readiness.summary
import org.obd.graphs.bl.readiness.update
import org.obd.graphs.preferences.CoreDialogFragment
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateString
import org.obd.graphs.ui.withDataLogger
import org.obd.metrics.api.model.ReadinessMonitor
import org.obd.metrics.api.model.ReadinessMonitorId
import java.util.Locale

// Polls the readiness query while open and puts back the query it replaced when closed.
internal class ReadinessDialogFragment : CoreDialogFragment() {
    private var state = ReadinessState()
    private var view = ReadinessView.SINCE_CLEAR
    private var replacedQuery: Query? = null

    private lateinit var mil: TextView
    private lateinit var summary: TextView
    private lateinit var monitors: LinearLayout
    private lateinit var message: TextView
    private lateinit var counters: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requestWindowFeatures()
        // Keeps the card off the screen edges; the window is otherwise as wide as the screen.
        val inset = resources.getDimensionPixelSize(R.dimen.readiness_dialog_side_inset)
        dialog?.window?.setBackgroundDrawable(InsetDrawable(ColorDrawable(Color.TRANSPARENT), inset, 0, inset, 0))
        val root = inflater.inflate(R.layout.dialog_readiness, container, false)

        mil = root.findViewById(R.id.readiness_mil)
        summary = root.findViewById(R.id.readiness_summary)
        monitors = root.findViewById(R.id.readiness_monitors)
        message = root.findViewById(R.id.readiness_message)
        counters = root.findViewById(R.id.readiness_counters)

        root.findViewById<RadioGroup>(R.id.readiness_view).setOnCheckedChangeListener { _, checkedId ->
            view = if (checkedId == R.id.readiness_view_drive_cycle) ReadinessView.DRIVE_CYCLE else ReadinessView.SINCE_CLEAR
            render()
        }
        attachCloseButton(root)
        attachHelp(root)

        // observeEach, not observe: one batch reply decodes 01 01 and 01 41 together, and LiveData kept only the last.
        DataLoggerRepository.observeEach(viewLifecycleOwner) { metric ->
            val pid = metric.command.pid
            val next = state.update(pid.id, metric.value, pid.units)
            if (next != state) {
                if (next.sinceClear != null && next.sinceClear != state.sinceClear) {
                    Prefs.updateString(PREF_READINESS_LAST, next.sinceClear!!.snapshot(System.currentTimeMillis()).encode())
                }
                state = next
                render()
            }
        }

        if (DataLoggerRepository.isRunning()) {
            replacedQuery = DataLoggerRepository.runningQuery()
            withDataLogger { updateQuery(Query.instance(QueryStrategyType.READINESS_QUERY)) }
        }

        render()
        return root
    }

    // How monitors relate to trouble codes, collapsed by default: it is read once, the monitors every time.
    private fun attachHelp(root: View) {
        val body = root.findViewById<TextView>(R.id.readiness_help)
        root.findViewById<TextView>(R.id.readiness_help_toggle).apply {
            fun label() = getString(R.string.readiness_help_title) + if (body.visibility == View.VISIBLE) " ▴" else " ▾"
            text = label()
            setOnClickListener {
                body.visibility = if (body.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                text = label()
            }
        }
    }

    override fun onDestroyView() {
        replacedQuery?.let { query ->
            if (DataLoggerRepository.isRunning()) withDataLogger { updateQuery(query) }
        }
        replacedQuery = null
        super.onDestroyView()
    }

    private fun render() {
        renderMil()

        val status = state.status(view)
        monitors.removeAllViews()
        if (status == null) {
            summary.visibility = View.GONE
            message.visibility = View.VISIBLE
            message.setText(
                when {
                    !DataLoggerRepository.isRunning() -> R.string.readiness_not_connected
                    state.isNotReported(view) -> R.string.readiness_not_reported
                    else -> R.string.readiness_no_data
                }
            )
        } else {
            val result = status.summary()
            summary.visibility = View.VISIBLE
            summary.text =
                if (result.allComplete) {
                    getString(R.string.readiness_all_complete)
                } else {
                    getString(R.string.readiness_summary, result.complete, result.supported)
                }
            summary.setTextColor(color(if (result.allComplete) R.color.philippine_green else R.color.dtc_state_active))

            val shown = status.shownMonitors()
            shown.forEach { monitors.addView(monitorRow(it)) }
            message.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
            message.setText(R.string.readiness_no_monitors)
        }
        renderCounters()
    }

    // The MIL and the emission code count come from 01 01 only; the drive-cycle reply does not carry them.
    private fun renderMil() {
        val sinceClear = state.sinceClear
        if (sinceClear == null) {
            mil.visibility = View.GONE
            return
        }
        mil.visibility = View.VISIBLE
        mil.text =
            listOf(
                getString(if (sinceClear.isMilOn) R.string.readiness_mil_on else R.string.readiness_mil_off),
                getString(R.string.readiness_emission_codes, sinceClear.emissionDtcCount)
            ).joinToString(" · ")
        mil.setTextColor(
            if (sinceClear.isMilOn) color(R.color.dtc_state_active) else ContextCompat.getColor(requireContext(), R.color.dialog_text_primary)
        )
    }

    private fun renderCounters() {
        val lines =
            READINESS_COUNTER_PIDS.mapNotNull { id ->
                state.counters[id]?.let { getString(counterLabel(id), format(it)) }
            }
        counters.visibility = if (lines.isEmpty()) View.GONE else View.VISIBLE
        counters.text = lines.joinToString("\n")
    }

    private fun monitorRow(monitor: ReadinessMonitor): TextView =
        TextView(requireContext()).apply {
            val tint = color(if (monitor.isComplete) R.color.philippine_green else R.color.dtc_state_active)
            text =
                getString(
                    R.string.readiness_monitor_row,
                    getString(monitorName(monitor.id)),
                    getString(if (monitor.isComplete) R.string.readiness_complete else R.string.readiness_incomplete)
                )
            textSize = 15f
            setTextColor(ContextCompat.getColor(context, R.color.dialog_text_primary))
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (monitor.isComplete) R.drawable.readiness_complete else R.drawable.readiness_incomplete,
                0,
                0,
                0
            )
            compoundDrawableTintList = ColorStateList.valueOf(tint)
            compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.readiness_row_icon_padding)
            val padding = resources.getDimensionPixelSize(R.dimen.readiness_row_padding)
            setPadding(0, padding, 0, padding)
        }

    private fun color(id: Int) = ContextCompat.getColor(requireContext(), id)

    private fun format(counter: ReadinessCounter): String {
        val value = counter.value.toDouble()
        val number = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.getDefault(), "%.1f", value)
        return listOfNotNull(number, counter.units?.takeIf { it.isNotBlank() }).joinToString(" ")
    }

    private fun counterLabel(id: Long): Int =
        when (id) {
            Pid.DISTANCE_SINCE_CODES_CLEARED_PID_ID.id -> R.string.readiness_counter_distance_since_clear
            Pid.TIME_SINCE_CODES_CLEARED_PID_ID.id -> R.string.readiness_counter_time_since_clear
            Pid.DISTANCE_MIL_ON_PID_ID.id -> R.string.readiness_counter_distance_mil_on
            else -> R.string.readiness_counter_time_mil_on
        }

    private fun monitorName(id: ReadinessMonitorId): Int =
        when (id) {
            ReadinessMonitorId.MISFIRE -> R.string.readiness_monitor_misfire
            ReadinessMonitorId.FUEL_SYSTEM -> R.string.readiness_monitor_fuel_system
            ReadinessMonitorId.COMPONENTS -> R.string.readiness_monitor_components
            ReadinessMonitorId.CATALYST -> R.string.readiness_monitor_catalyst
            ReadinessMonitorId.HEATED_CATALYST -> R.string.readiness_monitor_heated_catalyst
            ReadinessMonitorId.EVAPORATIVE_SYSTEM -> R.string.readiness_monitor_evaporative_system
            ReadinessMonitorId.SECONDARY_AIR -> R.string.readiness_monitor_secondary_air
            ReadinessMonitorId.AC_REFRIGERANT -> R.string.readiness_monitor_ac_refrigerant
            ReadinessMonitorId.OXYGEN_SENSOR -> R.string.readiness_monitor_oxygen_sensor
            ReadinessMonitorId.OXYGEN_SENSOR_HEATER -> R.string.readiness_monitor_oxygen_sensor_heater
            ReadinessMonitorId.NMHC_CATALYST -> R.string.readiness_monitor_nmhc_catalyst
            ReadinessMonitorId.NOX_SCR -> R.string.readiness_monitor_nox_scr
            ReadinessMonitorId.BOOST_PRESSURE -> R.string.readiness_monitor_boost_pressure
            ReadinessMonitorId.EXHAUST_GAS_SENSOR -> R.string.readiness_monitor_exhaust_gas_sensor
            ReadinessMonitorId.PM_FILTER -> R.string.readiness_monitor_pm_filter
            ReadinessMonitorId.EGR_VVT -> R.string.readiness_monitor_egr_vvt
        }
}
