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
package org.obd.graphs.bl.drag

const val VALUE_NOT_SET = -1L

data class DragRacingEntry(
    var current: Long = VALUE_NOT_SET,
    var last: Long = VALUE_NOT_SET,
    var best: Long = VALUE_NOT_SET,
    var currentSpeed: Int = VALUE_NOT_SET.toInt(),
    var ambientTemp: Int = VALUE_NOT_SET.toInt(),
    var atmPressure: Int = VALUE_NOT_SET.toInt()
)


data class DragRacingResults(
    var enableShiftLights: Boolean = false,
    var readyToRace: Boolean = false,
    var _0_100: DragRacingEntry = DragRacingEntry(),
    var _0_60: DragRacingEntry = DragRacingEntry(),
    var _0_160: DragRacingEntry = DragRacingEntry(),
    var _100_200: DragRacingEntry = DragRacingEntry(),
    var _60_140: DragRacingEntry = DragRacingEntry(),
)

data class DragRacingMetric(
    val time: Long,
    val speed: Int,
    val ambientTemp: Int? = 0,
    val atmPressure: Int? = 0
)