package org.obd.graphs.aa.renderer

import org.obd.graphs.aa.round
import java.util.*

private const val MAX_SIZE = 100
private const val NANOS = 1000000000.0

val fps = Fps()

class Fps {
    var times: LinkedList<Long> = object : LinkedList<Long>() {
        init {
            add(System.nanoTime())
        }
    }

    fun get(): Double {
        val lastTime = System.nanoTime()
        val difference = (lastTime - times.first) / NANOS

        times.addLast(lastTime)

        val size = times.size
        if (size > MAX_SIZE) {
            times.removeFirst()
        }

        return if (difference > 0) (times.size / difference).round(2) else 0.0
    }
}