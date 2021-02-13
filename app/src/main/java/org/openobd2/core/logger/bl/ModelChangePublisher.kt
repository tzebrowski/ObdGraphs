package org.openobd2.core.logger.bl

import androidx.lifecycle.MutableLiveData
import org.obd.metrics.ObdMetric
import org.obd.metrics.Reply
import org.obd.metrics.ReplyObserver
import org.obd.metrics.command.Command
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.command.obd.SupportedPidsCommand


internal class ModelChangePublisher : ReplyObserver() {

    var data: MutableMap<Command, ObdMetric> = hashMapOf()

    override fun onNext(reply: Reply<*>) {
        debugData.postValue(reply)
        if (reply.command is ObdCommand && reply.command !is SupportedPidsCommand) {
            data[reply.command] = reply as ObdMetric
            (reply.command as ObdCommand).pid?.let {
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