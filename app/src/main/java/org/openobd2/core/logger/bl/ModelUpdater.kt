package org.openobd2.core.logger.bl

import android.util.Log
import org.openobd2.core.CommandReplySubscriber
import org.openobd2.core.command.CommandReply
import org.openobd2.core.logger.Model

class ModelUpdater () : CommandReplySubscriber() {

    override fun onNext(reply: CommandReply<*>) {
        Log.i("DATA_LOGGER_ML", "$reply")
        Model.updateHomeStatus(reply.toString())
    }
}