package org.obd.graphs.bl.datalogger

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import org.obd.graphs.bl.trip.tripManager
import org.obd.metrics.api.model.*

internal class MetricsObserver : Lifecycle, ReplyObserver<Reply<*>>() {

    private val metrics: MutableLiveData<ObdMetric> = MutableLiveData<ObdMetric>()
    private val dynamicSelectorModeEventsBroadcaster = DynamicSelectorModeEventBroadcaster()

    override fun onStopped() {
        metrics.postValue(null)
        dynamicSelectorModeEventsBroadcaster.onStopped()
    }

    override fun onRunning(vehicleCapabilities: VehicleCapabilities?) {
        dynamicSelectorModeEventsBroadcaster.onRunning(vehicleCapabilities)
    }

    fun observe(lifecycleOwner: LifecycleOwner, observer: (metric: ObdMetric) -> Unit) {
        metrics.observe(lifecycleOwner){
            it?.let {
                observer(it)
            }
        }
    }
    override fun onNext(reply: Reply<*>) {

        if (reply is ObdMetric) {
            reply.command.pid?.let {
                dynamicSelectorModeEventsBroadcaster.postValue(reply)
                metrics.postValue(reply)
                tripManager.addTripEntry(reply)
            }
        }
    }
}