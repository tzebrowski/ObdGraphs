 /**
 * Copyright 2019-2026, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.renderer.cache

import kotlin.math.abs

internal class MetricStringCache(size: Int = 100) {
    private val pids = LongArray(size)
    private val values = DoubleArray(size)
    private val strings = Array<String?>(size) { null }
    private var count = 0

    // Define a small epsilon for safe floating-point comparison
    private val EPSILON = 0.0001

    inline fun get(pid: Long, value: Double, formatFallback: () -> String): String {
        for (i in 0 until count) {
            if (pids[i] == pid) {
                if (abs(values[i] - value) < EPSILON && strings[i] != null) {
                    return strings[i]!!
                }

                // Cache miss (value changed), update it
                val newStr = formatFallback()
                values[i] = value
                strings[i] = newStr
                return newStr
            }
        }

        // PID not found in cache, add it if there's room
        if (count < pids.size) {
            pids[count] = pid
            values[count] = value
            val newStr = formatFallback()
            strings[count] = newStr
            count++
            return newStr
        }

        // Cache is full, just format and return without caching
        return formatFallback()
    }
}
