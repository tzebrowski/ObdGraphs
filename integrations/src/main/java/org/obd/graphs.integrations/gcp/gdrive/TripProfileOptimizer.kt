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
package org.obd.graphs.integrations.gcp.gdrive

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class InputRoot(
    val startTs: Long,
    val entries: Map<String, InputEntryGroup>,
)

@Serializable
data class InputEntryGroup(
    val id: Int,
    val metrics: List<InputMetric>,
    val min: Double,
    val max: Double,
    val mean: Double,
)

@Serializable
data class InputMetric(
    val entry: InputInnerEntry,
    val ts: Long,
    val rawAnswer: String,
)

@Serializable
data class InputInnerEntry(
    val x: Double,
    val y: Double,
    val data: Int,
)

@Serializable
data class OutputRoot(
    val startTs: Long,
    val entries: Map<String, OutputEntryGroup>,
)

@Serializable
data class OutputEntryGroup(
    val metrics: List<OutputMetric>,
    val min: Double,
    val max: Double,
    val mean: Double,
)

@Serializable
data class OutputMetric(
    val v: Double,
    val t: Long,
)

internal interface TripProfileOptimizer {
    fun optimize(jsonInput: String): String
}

internal class DefaultTripOptimizer(
    private val jsonConfig: Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        },
) : TripProfileOptimizer {
    override fun optimize(jsonInput: String): String {
        try {
            val inputRoot = jsonConfig.decodeFromString<InputRoot>(jsonInput)

            val outputEntries =
                inputRoot.entries.mapValues { (_, entryGroup) ->
                    OutputEntryGroup(
                        min = entryGroup.min,
                        max = entryGroup.max,
                        mean = entryGroup.mean,
                        metrics =
                            entryGroup.metrics.map { inputMetric ->
                                OutputMetric(
                                    v = inputMetric.entry.y,
                                    t = inputMetric.ts,
                                )
                            },
                    )
                }

            val outputRoot =
                OutputRoot(
                    startTs = inputRoot.startTs,
                    entries = outputEntries,
                )

            return jsonConfig.encodeToString(outputRoot)
        } catch (e: Exception) {
            // Handle parsing errors gracefully or rethrow a custom exception
            throw IllegalArgumentException("Failed to optimize trip profile JSON", e)
        }
    }
}
