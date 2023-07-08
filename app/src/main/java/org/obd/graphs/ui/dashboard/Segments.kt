package org.obd.graphs.ui.dashboard

import org.obd.graphs.round
import java.util.*

class Segments(
    val numOfSegments: Int,
    private val minValue: Double,
    maxValue: Double
) {

    class Segment(from: Double, to: Double) {
        var from: Double = from.round(2)
        var to: Double = to.round(2)

        override fun toString(): String {
            return "$from - $to \n"
        }
    }

    private var segments: MutableList<Segment>

    init {
        this.segments = calculateSegments(minValue, maxValue)
    }

    fun indexOf(value: Double): Int {
        if (value <= minValue) {
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