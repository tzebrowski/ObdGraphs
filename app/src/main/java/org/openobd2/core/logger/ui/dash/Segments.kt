package org.openobd2.core.logger.ui.dash

import java.util.*

class Segments {

    class Segment {
        var from = 0
        var to = 0

        constructor(from: Int, to: Int) {
            this.from = from
            this.to = to
        }
    }

    private var segments: MutableList<Segment>;
    private val maxValue: Int

    val numOfSegments: Int

    constructor(numOfSegments: Int,minValue: Int, maxValue: Int) {
        this.numOfSegments = numOfSegments
        this.maxValue = maxValue
        this.segments = calculateSegments(minValue,maxValue)
    }

    fun indexOf(value: Int): Int {
        if (value == 0){
            return 0
        }else {
            for (i in segments.indices) {
                val r = segments[i]
                if (value >= r.from && value <= r.to) {
                    return i
                }
            }
        }
        return segments.size
    }

    fun to(): MutableList<Float> {
        val l: MutableList<Float> = arrayListOf()
        l.add(0f)
        for (i in segments) {
            l.add(i.to.toFloat())
        }
        return l
    }
  
    fun calculateSegments(from: Int, to: Int): MutableList<Segment> {
        var pFrom = from
        if (from < 0) pFrom *= -1

        val segmentSize = (pFrom + to) / numOfSegments
        val list: MutableList<Segment> = LinkedList()
        var cnt = from + segmentSize
        while (cnt <= to) {
            list.add(Segment(cnt - segmentSize, cnt - 1))
            cnt += segmentSize
        }
        return list
    }
}