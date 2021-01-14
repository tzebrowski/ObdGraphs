package org.openobd2.core.logger.bl

import org.openobd2.core.pid.PidRegistry

class Pids {

    lateinit var generic: PidRegistry
    lateinit var custom: PidRegistry

    init {

        Thread.currentThread().contextClassLoader
            .getResourceAsStream("generic.json").use { source ->
                generic = PidRegistry.builder().source(source).build()
            }

        Thread.currentThread().contextClassLoader
            .getResourceAsStream("alfa.json").use { source ->
                custom = PidRegistry.builder().source(source).build()
            }

    }

    companion object {
        @JvmStatic
        var instance: Pids =
            Pids()
    }
}