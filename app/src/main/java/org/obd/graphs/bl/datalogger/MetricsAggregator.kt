package org.obd.graphs.bl.datalogger

import androidx.lifecycle.MutableLiveData
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.Reply
import org.obd.metrics.api.model.ReplyObserver
import org.obd.metrics.command.obd.SupportedPidsCommand
import org.obd.graphs.bl.trip.TripManager

internal class MetricsAggregator : ReplyObserver<Reply<*>>() {

    companion object {
        @JvmStatic
        val metrics: MutableLiveData<ObdMetric> = MutableLiveData<ObdMetric>().apply {
        }
    }

    private val tripManager: TripManager by lazy { TripManager.INSTANCE }

    fun reset() {
        metrics.postValue(null)
    }

    override fun onNext(reply: Reply<*>) {
        if (reply is ObdMetric && reply.command !is SupportedPidsCommand) {
            reply.command.pid?.let {
                metrics.postValue(reply)
                tripManager.addTripEntry(reply)
            }
        }
    }
}