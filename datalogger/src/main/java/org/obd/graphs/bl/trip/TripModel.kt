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
package org.obd.graphs.bl.trip

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.mikephil.charting.data.Entry
import org.obd.metrics.transport.message.ConnectorResponse

data class TripFileDesc(
    val fileName: String,
    val profileId: String,
    val profileLabel: String,
    val startTime: String,
    val tripTimeSec: String
)

data class Metric(
    val entry: Entry,
    val ts: Long,
    val rawAnswer: ConnectorResponse
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SensorData(
    val id: Long,
    val metrics: MutableList<Metric>,
    var min: Number = 0,
    var max: Number = 0,
    var mean: Number = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorData

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Trip(val startTs: Long, val entries: MutableMap<Long, SensorData>)
