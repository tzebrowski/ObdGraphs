package org.openobd2.core.logger.bl

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.openobd2.core.CommandReplySubscriber
import org.openobd2.core.command.Command
import org.openobd2.core.command.CommandReply
import org.openobd2.core.command.obd.ObdCommand
import org.openobd2.core.command.obd.SupportedPidsCommand

internal class ModelChangePublisher : CommandReplySubscriber() {

    var data: MutableMap<Command, CommandReply<*>> = hashMapOf()

    override fun onNext(reply: CommandReply<*>) {

        Log.v(LOG_KEY, "${reply.command}")

        if (reply.command is ObdCommand && reply.command !is SupportedPidsCommand) {
            data[reply.command] = reply
            liveData.postValue(reply)
        }
    }

    companion object {
        @JvmStatic
        val liveData: MutableLiveData<CommandReply<*>> =  MutableLiveData<CommandReply<*>>().apply {
        }
    }
}