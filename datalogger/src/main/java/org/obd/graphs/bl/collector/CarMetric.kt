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

import org.obd.graphs.round
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.ValueType

data class CarMetric(
    var source: ObdMetric,
    var value: Number?,
    var min: Double,
    var max: Double,
    var mean: Double,
    var enabled: Boolean = true
) {
    companion object {
        fun newInstance(source: ObdMetric, value: Number, min: Double = 0.0, max: Double = 0.0, mean: Double = 0.0)
        = CarMetric(source, value = value, min = min, max = max, mean = mean, enabled = true)
    }

    fun toNumber(value: Double?): String {
        return toNumber(source.command.pid, value).toString()
    }

    fun isInAlert() : Boolean = source.isAlert


    fun valueToString(): String =
         if (source.value == null) {
            "No data"
        } else {
            toNumber(source.valueToDouble())
        }

    fun toFloat(): Float =
        if (source.value == null) {
            source.command.pid.min.toFloat()
        } else {
            source.valueToDouble().toFloat()
        }

    override fun equals(other: Any?): Boolean{
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as CarMetric

        return this.source == other.source
    }

    override fun hashCode(): Int{
        return this.source.hashCode()
    }
}

fun toNumber(pid: PidDefinition, input: Number?): Number {

    if (input == null) {
        return Double.NaN
    }

    val value = input.toDouble()
    if (value.isNaN()) {
        return 0.0
    }
    return if (pid.type == null) value.round(2) else
        pid.type.let {
            return when (pid.type) {
                ValueType.DOUBLE -> value.round(2)
                ValueType.INT -> value.toInt()
                ValueType.SHORT -> value.toInt()
                else -> value.round(1)
            }
        }
}