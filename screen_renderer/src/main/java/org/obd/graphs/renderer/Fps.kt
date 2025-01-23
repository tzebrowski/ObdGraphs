 /**
 * Copyright 2019-2025, Tomasz Żebrowski
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
package org.obd.graphs.renderer

import org.obd.graphs.round
import java.util.*


private const val MAX_SIZE = 100
private const val NANOS = 1000000000.0

class Fps {
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