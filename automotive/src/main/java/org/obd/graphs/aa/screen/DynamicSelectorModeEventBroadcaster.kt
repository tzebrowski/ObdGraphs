package org.obd.graphs.aa.screen

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.obd.graphs.MetricsProcessor
import org.obd.graphs.datalogger.BuildConfig
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.query.DYNAMIC_SELECTOR_PID_ID
import org.obd.graphs.query.isDynamicSelector
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.VehicleCapabilities
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.ValueType
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


const val EVENT_DYNAMIC_SELECTOR_MODE_NORMAL = "event.dynamic.selector.mode.normal"
const val EVENT_DYNAMIC_SELECTOR_MODE_ECO = "event.dynamic.selector.mode.eco"
const val EVENT_DYNAMIC_SELECTOR_MODE_SPORT = "event.dynamic.selector.mode.sport"
const val EVENT_DYNAMIC_SELECTOR_MODE_RACE = "event.dynamic.selector.mode.race"

private class FakeMetricsBroadcaster (private val emitter: DynamicSelectorModeEventBroadcaster) {

    private val values = listOf(2,2,2,2,0,0,0,0,0,4,4,4,4,4,4,2,2,2,2,0,0,0,0,0,4,4,4,4,4,4,2,2,2,2,0,0,0,0,0,4,4,4,4,4,4)
    private var broadcasterLaunched:Boolean = false

    fun onStopped() {
        broadcasterLaunched = false
    }
    fun onRunning() {
        if (!broadcasterLaunched){
            broadcasterLaunched = true
            val pid = PidDefinition(DYNAMIC_SELECTOR_PID_ID,1,"A","22","18F0","","",-1,10, ValueType.INT)
            var cnt = 0
            val executor: ExecutorService = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())

            executor.execute {
                while (broadcasterLaunched){
                    Thread.sleep(3000)
                    if (cnt == values.size){
                        cnt = 0
                    }
                    val metric = ObdMetric.builder().value(values[cnt]).command(ObdCommand(pid)).build()
                    emitter.postValue(metric)
                    cnt++
                }
                handler.post {}
            }

        }
    }
}

internal class DynamicSelectorModeEventBroadcaster: MetricsProcessor {
    private var currentMode = -1
    private val fakeEventsBroadcaster = FakeMetricsBroadcaster(this)

    override fun onStopped() {
        if (isBroadcastingFakeMetricsEnabled()){
            fakeEventsBroadcaster.onStopped()
        }
    }

    override fun onRunning(vehicleCapabilities: VehicleCapabilities?) {
        if (isBroadcastingFakeMetricsEnabled()) {
            fakeEventsBroadcaster.onRunning()
        }
    }

    override fun postValue(obdMetric: ObdMetric) {
        if (obdMetric.isDynamicSelector()) {

            if (Log.isLoggable(LOG_KEY, Log.VERBOSE)) {
                Log.v(LOG_KEY, "Received=${obdMetric.value.toInt()}, current=${currentMode} ")
            }

            if (currentMode != obdMetric.value) {

                if (Log.isLoggable(LOG_KEY, Log.VERBOSE)) {
                    Log.v(LOG_KEY, "Broadcasting Dynamic Selector Mode Change, new=${obdMetric.value.toInt()}")
                }

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

    private fun isBroadcastingFakeMetricsEnabled(): Boolean = BuildConfig.DEBUG &&
        Prefs.getBoolean("pref.debug.generator.broadcast_fake_metrics", false)
}