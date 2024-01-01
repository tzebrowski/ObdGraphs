package org.obd.graphs.bl.generator

import org.obd.graphs.bl.query.AMBIENT_TEMP_PID_ID
import org.obd.graphs.bl.query.ATM_PRESSURE_PID_ID
import org.obd.graphs.bl.query.DYNAMIC_SELECTOR_PID_ID
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.ValueType

internal data class MetricGeneratorDefinition(val pid: PidDefinition, val data: MutableList<Number>, var counter: Int = 0)

internal val baseMetrics = mutableMapOf(
    DYNAMIC_SELECTOR_PID_ID to MetricGeneratorDefinition(
        pid = PidDefinition(DYNAMIC_SELECTOR_PID_ID,1,"A","22","18F0","","",-1,10, ValueType.INT),
        data = mutableListOf<Number>().apply {
            (0..100).forEach{ _ ->
                add(0)
            }

            (0..150).forEach{ _ ->
                add(2)
            }

            (0..200).forEach{ _ ->
                add(4)
            }

    }),

    ATM_PRESSURE_PID_ID to MetricGeneratorDefinition(
        pid = PidDefinition(ATM_PRESSURE_PID_ID,1,"A","22","18F0","C","Atm Pressure",700,1200, ValueType.INT),
        data = mutableListOf<Number>().apply {
            (0..125).forEach{ _ ->
                add(1020)
            }
            (0..125).forEach{ _ ->
                add(999)
            }
        }),

    AMBIENT_TEMP_PID_ID to MetricGeneratorDefinition(
        pid = PidDefinition(AMBIENT_TEMP_PID_ID,1,"A","22","18F0","C","Ambient Temp",0,50, ValueType.INT),
        data = mutableListOf<Number>().apply {
            (5..25).forEach{
                add(it)
            }
        }),
)