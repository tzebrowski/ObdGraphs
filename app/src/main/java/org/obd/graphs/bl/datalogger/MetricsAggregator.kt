package org.obd.graphs.bl.datalogger

import androidx.lifecycle.MutableLiveData
import org.obd.metrics.ObdMetric
import org.obd.metrics.Reply
import org.obd.metrics.ReplyObserver
import org.obd.metrics.command.obd.SupportedPidsCommand
import org.obd.graphs.bl.trip.TripRecorder

internal class MetricsAggregator : ReplyObserver<Reply<*>>() {

    companion object {
        @JvmStatic
        val metrics: MutableLiveData<ObdMetric> = MutableLiveData<ObdMetric>().apply {
        }
    }

    private val tripRecorder: TripRecorder by lazy { TripRecorder.instance }

    fun reset() {
        metrics.postValue(null)
    }

    override fun onNext(reply: Reply<*>) {
        if (reply is ObdMetric && reply.command !is SupportedPidsCommand) {
            reply.command.pid?.let {
                metrics.postValue(reply)
                tripRecorder.addTripEntry(reply)
            }
        }
    }
}