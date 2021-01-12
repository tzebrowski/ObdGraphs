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

    val numOfSegments: Int
    var segments: MutableList<Segment>;
    val targetValue: Int

    constructor(numOfSegments: Int, targetValue: Int) {
        this.numOfSegments = numOfSegments
        this.targetValue = targetValue
        this.segments = calculateSegments(targetValue)
    }

    fun indexOf(value: Int): Int {
        if (value >= targetValue) {
            //last segment
            return segments.size;
        } else if (value == 0) {
            return 0;
        } else {
            for (i in segments.indices) {
                val r = this.segments[i]
                if (value >= r.from && value <= r.to) {
                    return i
                }
            }
            return -1
        }
    }

    fun to(): MutableList<Float> {
        val l: MutableList<Float> = arrayListOf()
        l.add(0f)
        for (i in segments) {
            l.add(i.to.toFloat())
        }
        return l
    }

    private fun calculateSegments(maxValue: Int): MutableList<Segment> {
        val setSize = maxValue / numOfSegments
        val list: MutableList<Segment> = LinkedList<Segment>()
        var cnt = setSize
        while (cnt <= maxValue) {
            list.add(Segment(cnt - setSize, cnt - 1))
            cnt += setSize
        }
        this.segments = list
        return list
    }
}