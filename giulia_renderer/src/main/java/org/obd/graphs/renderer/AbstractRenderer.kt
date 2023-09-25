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
package org.obd.graphs.renderer

import android.content.Context
import android.graphics.Rect
import org.obd.graphs.bl.collector.CarMetric
import kotlin.math.max

@Suppress("NOTHING_TO_INLINE")
abstract class AbstractRenderer(protected val settings: ScreenSettings, protected val context: Context, protected val fps: Fps)  : ScreenRenderer {

    protected inline fun splitIntoChunks(metrics: List<CarMetric>): MutableList<List<CarMetric>> {
        val lists = metrics.chunked(max(metrics.size / settings.getMaxColumns(), 1)).toMutableList()
        if (lists.size == 3) {
            lists[0] = lists[0]
            lists[1] = lists[1] + lists[2]
            lists.removeAt(2)
        }
        return lists
    }

    protected inline fun initialValueTop(area: Rect): Float =
        when (settings.getMaxColumns()) {
            1 -> area.left + ((area.width()) - 42).toFloat()
            else -> area.left + ((area.width() / 2) - 32).toFloat()
        }
}