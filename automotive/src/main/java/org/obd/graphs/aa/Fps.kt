package org.obd.graphs.aa

import java.util.*

private const val MAX_SIZE = 100
private const val NANOS = 1000000000.0

internal val fps = Fps()

internal class Fps {
    var times: LinkedList<Long> = LinkedList<Long>()

    fun start() {
        times.clear()
        times.add(System.nanoTime())
    }

    fun stop() {
        times.clear()
    }

    fun get(): Double {

        if (times.size == 0) {
            times.clear()
            return 0.0
        }

        val lastTime = System.nanoTime()
        val difference = (lastTime - times.first) / NANOS

        times.addLast(lastTime)

        val size = times.size
        if (size > MAX_SIZE) {
            times.removeFirst()
        }

        return if (difference > 0) (times.size / difference).round(3) else 0.0
    }
}