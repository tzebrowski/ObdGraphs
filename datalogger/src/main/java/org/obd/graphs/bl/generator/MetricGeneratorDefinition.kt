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
package org.obd.graphs.bl.generator

import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.query.namesRegistry
import org.obd.metrics.pid.PidDefinition

private const val VEHICLE_RUNNING_KEY = "vehicle.running"
private const val KEY_STATUS_KEY = "key.status"
private const val ENGINE_RUNNING_KEY = "engine.running"

private class ToStringHashMap<T, V>(private val converter: ToStringConverter<T, V>) : HashMap<T, V>() {
    fun interface ToStringConverter<TT, VV> {
        fun convert(items: Map<TT, VV>): String
    }

    override fun toString(): String {
        return converter.convert(this)
    }
}

private val toString: ToStringHashMap.ToStringConverter<String, Boolean> = object : ToStringHashMap.ToStringConverter<String, Boolean> {
    override fun convert(items: Map<String, Boolean>): String {
        if (items[VEHICLE_RUNNING_KEY]!!) {
            return "Running"
        }
        if (!items[VEHICLE_RUNNING_KEY]!! && items[ENGINE_RUNNING_KEY]!!) {
            return "Idling"
        }
        if (!items[VEHICLE_RUNNING_KEY]!! &&
            !items[ENGINE_RUNNING_KEY]!! &&
            items[KEY_STATUS_KEY]!!
        ) {
            return "Key on"
        }
        return if (!items[VEHICLE_RUNNING_KEY]!! &&
            !items[ENGINE_RUNNING_KEY]!! &&
            !items[KEY_STATUS_KEY]!!
        ) {
            "Key off"
        } else "Unknown"
    }
}


internal data class MetricGeneratorDefinition(val pid: PidDefinition, val data: MutableList<*>, var counter: Int = 0)

internal val baseMetrics = mutableMapOf(
    namesRegistry.getDynamicSelectorPID() to MetricGeneratorDefinition(
        pid = dataLogger.getPidDefinitionRegistry().findBy(namesRegistry.getDynamicSelectorPID()),
        data = mutableListOf<Number>().apply {
            (0..100).forEach{ _ ->
                add(0)
            }

            (0..150).forEach{ _ ->
                add(2)
            }

            (0..200).forEach{ _ ->
                add(4)
            }

    }),

    namesRegistry.getAtmPressurePID() to MetricGeneratorDefinition(
        pid = dataLogger.getPidDefinitionRegistry().findBy(namesRegistry.getAtmPressurePID()),
        data = mutableListOf<Number>().apply {
            (0..125).forEach{ _ ->
                add(1020)
            }
            (0..125).forEach{ _ ->
                add(999)
            }
        }),

    namesRegistry.getAmbientTempPID() to MetricGeneratorDefinition(
        pid = dataLogger.getPidDefinitionRegistry().findBy(namesRegistry.getAmbientTempPID()),
        data = mutableListOf<Number>().apply {
            (5..25).forEach{
                add(it)
            }
        }),

    namesRegistry.getVehicleSpeedPID() to MetricGeneratorDefinition(
        pid = dataLogger.getPidDefinitionRegistry().findBy(namesRegistry.getVehicleSpeedPID()),
        data = mutableListOf<Number>().apply {
            (0..100).forEach{ _ ->
                add(0)
            }
            (1..100).forEach{
                add(it)
            }

        }),

    namesRegistry.getVehicleStatusPID() to MetricGeneratorDefinition(
        pid = dataLogger.getPidDefinitionRegistry().findBy(namesRegistry.getVehicleStatusPID()),
        data = mutableListOf<Map<String,Boolean>>().apply {

            (0..100).forEach{ _ ->
                val map = ToStringHashMap(toString)
                map["vehicle.running"] = true
                map["vehicle.accelerating"] = false
                map["vehicle.decelerating"] = false
                map["key.status"] = true
                map["engine.running"] = true
                add(map)
            }
            (1..100).forEach{ _ ->
                val map = ToStringHashMap(toString)
                map["vehicle.running"] = false
                map["vehicle.accelerating"] = false
                map["vehicle.decelerating"] = false
                map["key.status"] = true
                map["engine.running"] = true
                add(map)
            }

            (1..100).forEach{ _ ->
                val map =  ToStringHashMap(toString)
                map["vehicle.running"] = false
                map["vehicle.accelerating"] = false
                map["vehicle.decelerating"] = false
                map["key.status"] = true
                map["engine.running"] = false
                add(map)
            }
        }),
)

