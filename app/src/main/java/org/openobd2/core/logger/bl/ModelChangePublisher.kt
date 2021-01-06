package org.openobd2.core.logger.bl

import android.util.Log
import org.openobd2.core.CommandReplySubscriber
import org.openobd2.core.command.Command
import org.openobd2.core.command.CommandReply
import org.openobd2.core.command.obd.ObdCommand
import org.openobd2.core.command.obd.SupportedPidsCommand
import org.openobd2.core.logger.Model

class ModelChangePublisher () : CommandReplySubscriber() {

    var data: MutableMap<Command,CommandReply<*>> = hashMapOf<Command,CommandReply<*>>()

    override fun onNext(reply: CommandReply<*>) {
        Log.i("DATA_LOGGER_ML", "$reply")

        data[reply.command] = reply

        Model.updateDebugScreen(reply.toString())

        if (reply.command is ObdCommand && reply.command !is SupportedPidsCommand) {
            Model.udpateMetricsScreen(reply)
        }
    }
}