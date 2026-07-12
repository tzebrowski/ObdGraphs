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

sealed class PidDefinitionDialogMode {
    object Edit : PidDefinitionDialogMode()
    object Alert : PidDefinitionDialogMode()
    object TripInfo : PidDefinitionDialogMode()
    object Performance : PidDefinitionDialogMode()
    object LowPriority : PidDefinitionDialogMode()
    object HighPriority : PidDefinitionDialogMode()
    object Dashboard : PidDefinitionDialogMode()
    object Graph : PidDefinitionDialogMode()
    object Gauge : PidDefinitionDialogMode()
    object Giulia : PidDefinitionDialogMode()
    object AA : PidDefinitionDialogMode()
    data class Custom(val id: String) : PidDefinitionDialogMode()

    val isInteractive: Boolean
        get() = this is Edit || this is Alert

    val isEdit: Boolean
        get() = this is Edit

    val isAlert: Boolean
        get() = this is Alert

    companion object {
        fun fromString(source: String?): PidDefinitionDialogMode = when (source) {
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
