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


val dragRacingResultRegistry: DragRacingResultRegistry by lazy { InMemoryDragRacingRegistry() }



interface DragRacingResultRegistry {
    fun getResult(): DragRacingResults
    fun update0100(result: DragRacingMetric)
    fun update0160(result: DragRacingMetric)
    fun update060(result: DragRacingMetric)
    fun update100200(result: DragRacingMetric)
    fun update60140(result: DragRacingMetric)
    fun readyToRace(value: Boolean)
    fun enableShiftLights(value: Boolean)

    fun getShiftLightsRevThreshold(): Int
    fun setShiftLightsRevThreshold(newThresholdValue: Int)
}