package org.obd.graphs.bl.datalogger

import androidx.lifecycle.MutableLiveData
import org.obd.graphs.bl.trip.TripManager
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.Reply
import org.obd.metrics.api.model.ReplyObserver

internal class MetricsCollector : ReplyObserver<Reply<*>>() {

    val metrics: MutableLiveData<ObdMetric> = MutableLiveData<ObdMetric>()

    private val tripManager: TripManager by lazy { TripManager.INSTANCE }

    fun reset() {
        metrics.postValue(null)
    }

    override fun onNext(reply: Reply<*>) {
        if (reply is ObdMetric) {
            reply.command.pid?.let {
                metrics.postValue(reply)
                tripManager.addTripEntry(reply)
            }
        }
    }
}