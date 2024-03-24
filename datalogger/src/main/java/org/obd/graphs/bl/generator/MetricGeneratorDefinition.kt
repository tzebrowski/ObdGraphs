package org.obd.graphs.bl.generator

import org.obd.graphs.bl.query.namesRegistry
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.ValueType

internal data class MetricGeneratorDefinition(val pid: PidDefinition, val data: MutableList<Number>, var counter: Int = 0)

internal val baseMetrics = mutableMapOf(
    namesRegistry.getDynamicSelectorPID() to MetricGeneratorDefinition(
        pid = PidDefinition(namesRegistry.getDynamicSelectorPID(),1,"A","22","18F0","","",-1,10, ValueType.INT),
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

    namesRegistry.getAtmPressurePID() to MetricGeneratorDefinition(
        pid = PidDefinition( namesRegistry.getAtmPressurePID(),1,"A","22","18F0","C","Atm Pressure",700,1200, ValueType.INT),
        data = mutableListOf<Number>().apply {
            (0..125).forEach{ _ ->
                add(1020)
            }
            (0..125).forEach{ _ ->
                add(999)
            }
        }),

    namesRegistry.getAmbientTempPID() to MetricGeneratorDefinition(
        pid = PidDefinition( namesRegistry.getAmbientTempPID(),1,"A","22","18F0","C","Ambient Temp",0,50, ValueType.INT),
        data = mutableListOf<Number>().apply {
            (5..25).forEach{
                add(it)
            }
        }),

    namesRegistry.getVehicleSpeedPID() to MetricGeneratorDefinition(
        pid = PidDefinition(namesRegistry.getVehicleSpeedPID(),2,"A","22","18F0","km/h","Vehicle Speed",0,300, ValueType.INT),
        data = mutableListOf<Number>().apply {
            (0..100).forEach{ _ ->
                add(0)
            }
            (1..100).forEach{
                add(it)
            }

        }),
)