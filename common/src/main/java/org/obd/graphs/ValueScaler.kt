/**
 * Copyright 2019-2024, Tomasz Żebrowski
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
package org.obd.graphs

import org.obd.metrics.api.model.ObdMetric

const val NEW_RANGE_MIN_VAL = 0f
const val NEW_RANGE_MAX_VAL = 3500f

class ValueScaler {
    fun scaleToNewRange(
        obdMetric: ObdMetric
    ): Float {
        return scaleToNewRange(
            obdMetric.valueToDouble().toFloat(), obdMetric.command.pid.min.toFloat(),
            obdMetric.command.pid.max.toFloat(), NEW_RANGE_MIN_VAL, NEW_RANGE_MAX_VAL
        )
    }

    fun scaleToNewRange(
        currentValue: Float,
        currentMin: Float,
        currentMax: Float,
        targetMin: Float,
        targetMax: Float
    ): Float {
        return (currentValue - currentMin) * (targetMax - targetMin) / (currentMax - currentMin) + targetMin
    }
}