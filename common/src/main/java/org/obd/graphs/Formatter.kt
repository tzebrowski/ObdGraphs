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
package org.obd.graphs

import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.ValueType

private const val NO_DATA = "No data"

fun ObdMetric.format(
    castToInt: Boolean = false,
    precision: Int = 2,
): String =
    format(
        input = this.value,
        pid = null,
        precision = precision,
        castToInt = castToInt,
    )

fun ObdMetric.toFloat(): Float =
    if (this.value == null) {
        if (this.command.pid.min == null) {
            0f
        } else {
            this.command.pid.min
                .toFloat()
        }
    } else {
        if (this.value is Number && !(this.value as Number).toDouble().isNaN()) {
            this.valueToDouble().toFloat()
        } else {
            0f
        }
    }

fun ObdMetric.toInt(): Int =
    if (isNumber()) {
        (value as Number).toInt()
    } else {
        0
    }

fun ObdMetric.toDouble(): Double = (toNumber() ?: 0).toDouble()

fun ObdMetric.isNumber(): Boolean = this.value != null && this.value is Number

private fun ObdMetric.toNumber(): Number? =
    if (isNumber()) {
        value as Number
    } else {
        null
    }

fun Number.format(
    pid: PidDefinition,
    precision: Int = 2,
    castToInt: Boolean = false,
): String =
    format(
        input = this,
        pid = pid,
        precision = precision,
        castToInt = castToInt,
    )

private fun format(
    input: Any?,
    pid: PidDefinition? = null,
    precision: Int = 2,
    castToInt: Boolean = false,
): String =

    if (input == null) {
        NO_DATA
    } else {
        if (input is Number) {
            val number =
                if (pid == null || pid.type == null) {
                    if (castToInt) {
                        input.toInt()
                    } else if (input is Double) {
                        (input as Number).toDouble().round(precision)
                    } else {
                        input
                    }
                } else {
                    pid.type.let {
                        when (pid.type) {
                            ValueType.DOUBLE -> input.toDouble().round(precision)
                            ValueType.INT -> input.toInt()
                            ValueType.SHORT -> input.toInt()
                            else -> input.toDouble().round(1)
                        }
                    }
                }

            if (number is Double && number.toDouble().isNaN()) {
                NO_DATA
            } else {
                number.toString()
            }
        } else {
            input.toString()
        }
    }
