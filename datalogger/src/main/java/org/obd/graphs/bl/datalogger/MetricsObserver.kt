package org.obd.graphs.bl.datalogger

import androidx.lifecycle.MutableLiveData
import org.obd.graphs.bl.trip.tripManager
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.Reply
import org.obd.metrics.api.model.ReplyObserver

internal class MetricsObserver : ReplyObserver<Reply<*>>() {
    internal val metrics: MutableLiveData<ObdMetric> = MutableLiveData<ObdMetric>()
    private val  dynamicSelectorModeEvenEmitter = DynamicSelectorModeEvenEmitter()
    fun reset() {
        metrics.postValue(null)
    }
    override fun onNext(reply: Reply<*>) {
        if (reply is ObdMetric) {
            reply.command.pid?.let {
                dynamicSelectorModeEvenEmitter.postValue(reply)
                metrics.postValue(reply)
                tripManager.addTripEntry(reply)
            }
        }
    }
}