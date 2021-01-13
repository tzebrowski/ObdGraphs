package org.openobd2.core.logger.bl

import org.openobd2.core.pid.PidRegistry

class PidsRegistry {

    lateinit var genericRegistry: PidRegistry
    lateinit var mode22Registry: PidRegistry

    init {

        Thread.currentThread().contextClassLoader
            .getResourceAsStream("generic.json").use { source ->
                genericRegistry = PidRegistry.builder().source(source).build()
            }

        Thread.currentThread().contextClassLoader
            .getResourceAsStream("alfa.json").use { source ->
                mode22Registry = PidRegistry.builder().source(source).build()
            }

    }

    companion object {
        @JvmStatic
        var instance: PidsRegistry =
            PidsRegistry()
    }

}