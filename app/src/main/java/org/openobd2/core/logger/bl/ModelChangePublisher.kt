package org.openobd2.core.logger.bl

import android.util.Log
import org.openobd2.core.CommandReplySubscriber
import org.openobd2.core.command.Command
import org.openobd2.core.command.CommandReply
import org.openobd2.core.command.obd.ObdCommand
import org.openobd2.core.command.obd.SupportedPidsCommand
import org.openobd2.core.logger.Model

internal class ModelChangePublisher : CommandReplySubscriber() {

    var data: MutableMap<Command, CommandReply<*>> = hashMapOf()

    override fun onNext(reply: CommandReply<*>) {

        Log.v(LOG_KEY, "${reply.command}")

        Model.updateDebugScreen(reply.toString())
        if (reply.command is ObdCommand && reply.command !is SupportedPidsCommand) {
            data[reply.command] = reply
            Model.updateLiveData(reply)
        }
    }
}