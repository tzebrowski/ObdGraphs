package org.openobd2.core.logger.ui.dash

import org.openobd2.core.command.CommandReply
import java.util.*
import kotlin.math.round

fun CommandReply<*>.valueAsString(): String {
    return if (this.value == null) {
        ""
    } else {
        if (value.toString().contains(".")) {
            value.toString().toDouble().round(2).toString()
        } else {
            value.toString().toInt().toString()
        }
    }
}

fun CommandReply<*>.valueToDouble(): Double {
    return when (value) {
        null -> 0.0
        else -> this.value.toString().toDouble().round(2)
    }
}

fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}


class Segments {

    class Segment {
        var from: Double
        var to: Double

        constructor(from: Double, to: Double) {
            this.from = from.round(2)
            this.to = to.round(2)
        }

        override fun toString(): String {
            return "$from - $to \n"
        }
    }

    private var segments: MutableList<Segment>
    private val maxValue: Double

    val numOfSegments: Int

    constructor(numOfSegments: Int, minValue: Double, maxValue: Double) {
        this.numOfSegments = numOfSegments
        this.maxValue = maxValue
        this.segments = calculateSegments(minValue, maxValue)
    }

    fun indexOf(value: Double): Int {
        if (value.equals(0)) {
            return 0
        } else {
            for (i in segments.indices) {
                val r = segments[i]
                if (value >= r.from && value <= r.to) {
                    return i
                }
            }
        }
        return segments.size
    }

    fun to(): MutableList<Double> {
        val l: MutableList<Double> = arrayListOf()
        l.add(0.0)
        for (i in segments) {
            l.add(i.to)
        }
        return l
    }


    private fun calculateSegments(from: Double, to: Double): MutableList<Segment> {
        var pFrom = from
        if (from < 0) pFrom *= -1

        val segmentSize = (pFrom + to) / numOfSegments
        val list: MutableList<Segment> = LinkedList()
        var cnt = from + segmentSize
        while (cnt <= to) {
            list.add(Segment(cnt - segmentSize, (cnt - 0.01)))
            cnt += segmentSize
        }
        return list
    }
}