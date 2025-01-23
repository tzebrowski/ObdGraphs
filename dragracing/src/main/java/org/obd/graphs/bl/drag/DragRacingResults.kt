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
package org.obd.graphs.bl.drag

const val VALUE_NOT_SET = -1L

data class DragRacingEntry(
    var current: Long = VALUE_NOT_SET,
    var last: Long = VALUE_NOT_SET,
    var best: Long = VALUE_NOT_SET,
    var bestAmbientTemp: Int = VALUE_NOT_SET.toInt(),
    var bestAtmPressure: Int = VALUE_NOT_SET.toInt(),
    var currentSpeed: Int = VALUE_NOT_SET.toInt()
)


data class DragRacingResults(
    var enableShiftLights: Boolean = false,
    var readyToRace: Boolean = false,
    var _0_100: DragRacingEntry = DragRacingEntry(),
    var _0_60: DragRacingEntry = DragRacingEntry(),
    var _0_160: DragRacingEntry = DragRacingEntry(),
    var _100_200: DragRacingEntry = DragRacingEntry(),
    var _60_140: DragRacingEntry = DragRacingEntry(),
    var ambientTemp: Int = VALUE_NOT_SET.toInt(),
    var atmPressure: Int = VALUE_NOT_SET.toInt()
)

data class DragRacingMetric(
    var time: Long,
    var speed: Int,
    var ambientTemp: Int? = 0,
    var atmPressure: Int? = 0
)