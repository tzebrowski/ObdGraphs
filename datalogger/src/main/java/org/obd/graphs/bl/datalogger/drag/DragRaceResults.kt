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
package org.obd.graphs.bl.datalogger.drag

const val VALUE_NOT_SET = -1L

data class DragRaceEntry(
    var _0_100ms: Long = VALUE_NOT_SET, var _0_160ms: Long = VALUE_NOT_SET, var _100_200ms: Long = VALUE_NOT_SET,
    var _0_100speed: Int = VALUE_NOT_SET.toInt(), var _0_160speed: Int = VALUE_NOT_SET.toInt(), var _100_200speed: Int = VALUE_NOT_SET.toInt()
)

data class DragRaceResults(
    val current: DragRaceEntry = DragRaceEntry(),
    val last: DragRaceEntry = DragRaceEntry(),
    val best: DragRaceEntry = DragRaceEntry()
)