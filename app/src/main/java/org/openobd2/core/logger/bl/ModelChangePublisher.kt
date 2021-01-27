package org.openobd2.core.logger.bl

import androidx.lifecycle.MutableLiveData
import org.obd.metrics.Metric
import org.obd.metrics.MetricsObserver
import org.obd.metrics.command.Command
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.command.obd.SupportedPidsCommand


internal class ModelChangePublisher : MetricsObserver() {

    var data: MutableMap<Command, Metric<*>> = hashMapOf()

    override fun onNext(reply: Metric<*>) {
        debugData.postValue(reply)
        if (reply.command is ObdCommand && reply.command !is SupportedPidsCommand) {
            data[reply.command] = reply
            (reply.command as ObdCommand).pid?.let {
                liveData.postValue(reply)
            }
        }
    }

    companion object {
        @JvmStatic
        val debugData: MutableLiveData<Metric<*>> = MutableLiveData<Metric<*>>().apply {
        }

        @JvmStatic
        val liveData: MutableLiveData<Metric<*>> = MutableLiveData<Metric<*>>().apply {
        }
    }
}