package org.obd.graphs.aa.screen

import android.util.Log
import org.obd.graphs.bl.datalogger.MetricsProcessor
import org.obd.graphs.bl.query.isDynamicSelector
import org.obd.graphs.bl.query.valueToNumber
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.api.model.ObdMetric


const val EVENT_DYNAMIC_SELECTOR_MODE_NORMAL = "event.dynamic.selector.mode.normal"
const val EVENT_DYNAMIC_SELECTOR_MODE_ECO = "event.dynamic.selector.mode.eco"
const val EVENT_DYNAMIC_SELECTOR_MODE_SPORT = "event.dynamic.selector.mode.sport"
const val EVENT_DYNAMIC_SELECTOR_MODE_RACE = "event.dynamic.selector.mode.race"

internal val dynamicSelectorModeEventBroadcaster:MetricsProcessor  = DynamicSelectorModeEventBroadcaster()

private const val LOG_TAG = "DynSelectorBroadcast"

internal class DynamicSelectorModeEventBroadcaster: MetricsProcessor {
    private var currentMode = -1

    override fun postValue(obdMetric: ObdMetric) {
        if (obdMetric.isDynamicSelector()) {

            if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
                Log.v(LOG_TAG, "Received=${obdMetric.valueToNumber()!!.toInt()}, current=${currentMode} ")
            }

            if (currentMode != obdMetric.valueToNumber()!!) {

                if (Log.isLoggable(LOG_TAG, Log.VERBOSE)) {
                    Log.v(LOG_TAG, "Broadcasting Dynamic Selector Mode Change, new=${obdMetric.valueToNumber()!!.toInt()}")
                }

                currentMode = obdMetric.valueToNumber()!!.toInt()
                when (obdMetric.valueToNumber()!!.toInt()) {
                    0 -> sendBroadcastEvent(EVENT_DYNAMIC_SELECTOR_MODE_NORMAL)
                    2 -> sendBroadcastEvent(EVENT_DYNAMIC_SELECTOR_MODE_SPORT)
                    4 -> sendBroadcastEvent(EVENT_DYNAMIC_SELECTOR_MODE_ECO)
                    else -> sendBroadcastEvent(EVENT_DYNAMIC_SELECTOR_MODE_RACE)
                }
            }
        }
    }
}