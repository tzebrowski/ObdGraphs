/*
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
package org.obd.graphs.preferences.pid

sealed class PidDialogMode {
    object Edit : PidDialogMode()
    object Alert : PidDialogMode()
    object TripInfo : PidDialogMode()
    object Performance : PidDialogMode()
    object LowPriority : PidDialogMode()
    object HighPriority : PidDialogMode()
    object Dashboard : PidDialogMode()
    object Graph : PidDialogMode()
    object Gauge : PidDialogMode()
    object Giulia : PidDialogMode()
    object AA : PidDialogMode()
    data class Custom(val id: String) : PidDialogMode()

    val isInteractive: Boolean
        get() = this is Edit || this is Alert

    val isEdit: Boolean
        get() = this is Edit

    val isAlert: Boolean
        get() = this is Alert

    companion object {
        fun fromString(source: String?): PidDialogMode = when (source) {
            "edit" -> Edit
            "alert" -> Alert
            "trip_info" -> TripInfo
            "performance" -> Performance
            "low" -> LowPriority
            "high" -> HighPriority
            "dashboard" -> Dashboard
            "graph" -> Graph
            "gauge" -> Gauge
            "giulia" -> Giulia
            "aa" -> AA
            else -> Custom(source ?: "")
        }
    }
}
