package org.openobd2.core.logger.bl

import androidx.lifecycle.MutableLiveData
import org.obd.metrics.CommandReplySubscriber
import org.obd.metrics.command.Command
import org.obd.metrics.command.CommandReply
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.command.obd.SupportedPidsCommand

internal class ModelChangePublisher : CommandReplySubscriber() {

    var data: MutableMap<Command, CommandReply<*>> = hashMapOf()

    override fun onNext(reply: CommandReply<*>) {
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
        val debugData: MutableLiveData<CommandReply<*>> =  MutableLiveData<CommandReply<*>>().apply {
        }

        @JvmStatic
        val liveData: MutableLiveData<CommandReply<*>> =  MutableLiveData<CommandReply<*>>().apply {
        }
    }
}