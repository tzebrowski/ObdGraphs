package org.openobd2.core.logger.bl

import androidx.lifecycle.MutableLiveData
import org.obd.metrics.ObdMetric
import org.obd.metrics.Reply
import org.obd.metrics.ReplyObserver
import org.obd.metrics.command.Command
import org.obd.metrics.command.obd.SupportedPidsCommand


internal class MetricsAggregator : ReplyObserver() {

    private val data: MutableMap<Command, ObdMetric> = hashMapOf()

    fun reset() {
        data.clear()
        debugData.postValue(null)
        metrics.postValue(null)
    }

    override fun onNext(reply: Reply<*>) {
        debugData.postValue(reply)
        if (reply is ObdMetric && reply.command !is SupportedPidsCommand) {
            data[reply.command] = reply
            reply.command.pid?.let {
                metrics.postValue(reply)
            }
        }
    }

    companion object {
        @JvmStatic
        val debugData: MutableLiveData<Reply<*>> = MutableLiveData<Reply<*>>().apply {
        }

        @JvmStatic
        val metrics: MutableLiveData<ObdMetric> = MutableLiveData<ObdMetric>().apply {
        }
    }
}