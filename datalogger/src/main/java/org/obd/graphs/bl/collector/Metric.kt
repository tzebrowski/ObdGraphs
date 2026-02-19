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
package org.obd.graphs.bl.collector

import org.obd.graphs.modules
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.pid.PidDefinition

data class Metric(
    var source: ObdMetric,
    var value: Any?,
    var min: Double,
    var max: Double,
    var mean: Double,
    var enabled: Boolean = true,
    var rate: Double?,
    var inLowerAlertRisedHist: Boolean = false,
    var inUpperAlertRisedHist: Boolean = false) {

    companion object {
        fun newInstance(source: ObdMetric, value: Any, min: Double = 0.0, max: Double = 0.0, mean: Double = 0.0) =
            Metric(source, value = value, min = min, max = max, mean = mean, enabled = true, rate = 0.0)
    }

    fun pid(): PidDefinition = source.command.pid

    fun moduleName(): String? =  modules.getDefaultModules()[pid().resourceFile]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Metric

        return this.source == other.source
    }

    override fun hashCode(): Int {
        return this.source.hashCode()
    }
}
