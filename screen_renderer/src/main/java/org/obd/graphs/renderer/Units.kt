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
package org.obd.graphs.renderer

// The PID definitions (ObdMetrics resources) spell temperatures as a bare "C"; they are shared with
// exports and logs, so the degree sign is added only where the unit is drawn.
private val DISPLAY_UNITS =
    mapOf(
        "C" to "°C",
        "F" to "°F"
    )

/** [units] as drawn on screen. */
internal fun displayUnits(units: String?): String? = units?.let { DISPLAY_UNITS[it.trim()] ?: it }
