package org.obd.graphs.bl.datalogger

import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.api.model.ObdMetric

private const val DYNAMIC_SELECTOR_PID_ID = 7036L

internal class DynamicSelectorModeEvenEmitter() {
    private var currentMode = -1

    fun postValue(obdMetric: ObdMetric) {
        if (isDynamicSelectorPID(obdMetric)) {

            if (currentMode != obdMetric.value) {
                currentMode = obdMetric.value.toInt()
                when (obdMetric.value.toInt()) {
                    0 -> sendBroadcastEvent(EVENT_DYNAMIC_SELECTOR_MODE_NORMAL)
                    2 -> sendBroadcastEvent(EVENT_DYNAMIC_SELECTOR_MODE_SPORT)
                    4 -> sendBroadcastEvent(EVENT_DYNAMIC_SELECTOR_MODE_ECO)
                    else -> sendBroadcastEvent(EVENT_DYNAMIC_SELECTOR_MODE_RACE)
                }
            }
        }
    }

    private fun isDynamicSelectorPID(obdMetric: ObdMetric): Boolean =  obdMetric.command.pid.id ==  DYNAMIC_SELECTOR_PID_ID
}