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
package org.obd.graphs.bl.collector

import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.metrics.api.model.ObdMetric

interface CarMetricsCollector {

    fun getMetrics(enabled: Boolean = true): List<CarMetric>

    fun findById(id: Long): CarMetric?

    fun applyFilter(enabled: Set<Long>, query: Set<Long> = dataLoggerPreferences.getPIDsToQuery(),
                    order: Map<Long, Int>? = null)

    fun append(input: ObdMetric?)

    companion object {
        fun instance () : CarMetricsCollector = InMemoryCarMetricsCollector()
    }
}