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

    constructor(numOfSegments: Int, maxValue: Int) {
        this.numOfSegments = numOfSegments
        this.maxValue = maxValue
        this.segments = calculateSegments(maxValue)
    }

    fun indexOf(value: Int): Int {
        if (value == 0){
            return 0
        }
        val indexOf: Int = value / (maxValue / numOfSegments);
        if (indexOf > numOfSegments){
            return numOfSegments;
        }
        return  indexOf
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
        val segmentSize = maxValue / numOfSegments
        val list: MutableList<Segment> = LinkedList<Segment>()
        var cnt = segmentSize
        while (cnt <= maxValue) {
            list.add(Segment(cnt - segmentSize, cnt - 1))
            cnt += segmentSize
        }
        return list
    }
}