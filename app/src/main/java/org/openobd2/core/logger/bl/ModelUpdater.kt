package org.openobd2.core.logger.bl

import org.openobd2.core.CommandReplySubscriber
import org.openobd2.core.command.CommandReply
import org.openobd2.core.logger.Model

class ModelUpdater () : CommandReplySubscriber() {

    override fun onNext(reply: CommandReply<*>) {
        Model.updateHomeStatus(reply.toString())
    }
}