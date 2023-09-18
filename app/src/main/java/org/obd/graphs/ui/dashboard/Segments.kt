/**
 * Copyright 2019-2023, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
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