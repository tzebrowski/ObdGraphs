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

import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.isUserCustom
import org.obd.metrics.pid.PidDefinition

enum class PidCategory(val stringRes: Int) {
    BASICS(R.string.pref_pid_manage_dialog_category_basics),
    IGNITION(R.string.pref_pid_manage_dialog_category_ignition),
    FUEL_AFR(R.string.pref_pid_manage_dialog_category_fuel_afr),
    BOOST(R.string.pref_pid_manage_dialog_category_boost),
    LOAD_TORQUE(R.string.pref_pid_manage_dialog_category_load_torque),
    TEMPERATURE(R.string.pref_pid_manage_dialog_category_temperature),
    AIR_INTAKE(R.string.pref_pid_manage_dialog_category_air_intake),
    ELECTRICAL(R.string.pref_pid_manage_dialog_category_electrical),
    LOCATION(R.string.pref_pid_manage_dialog_category_location),
    OTHER(R.string.pref_pid_manage_dialog_category_other)
}

// Order in which categories are grouped/sorted when browsing PIDs -- distinct from RULES below,
// which is match-priority order (most-specific keyword wins).
val PID_CATEGORY_DISPLAY_ORDER: List<PidCategory> = listOf(
    PidCategory.BASICS,
    PidCategory.IGNITION,
    PidCategory.FUEL_AFR,
    PidCategory.BOOST,
    PidCategory.LOAD_TORQUE,
    PidCategory.TEMPERATURE,
    PidCategory.AIR_INTAKE,
    PidCategory.ELECTRICAL,
    PidCategory.LOCATION,
    PidCategory.OTHER
)

private data class CategoryRule(val category: PidCategory, val pattern: Regex)

// Ordered most-specific first, e.g. "O2 Voltage" must match FUEL_AFR before the generic
// "voltage" keyword in ELECTRICAL, and "Calculated horse power" must match LOAD_TORQUE.
private val RULES: List<CategoryRule> = listOf(
    CategoryRule(PidCategory.IGNITION, Regex("spark|ignition|timing adv|misfire", RegexOption.IGNORE_CASE)),
    CategoryRule(PidCategory.FUEL_AFR, Regex("\\bafr\\b|lambda|fuel|air.?fuel|\\bo2\\b|oxygen", RegexOption.IGNORE_CASE)),
    CategoryRule(
        PidCategory.BOOST,
        Regex("boost|wastegate|turbo|manifold|intake pressure|\\bmap\\b|atmospheric|baro", RegexOption.IGNORE_CASE)
    ),
    CategoryRule(PidCategory.LOAD_TORQUE, Regex("torque|\\bload\\b|horsepower|horse power|\\bhp\\b", RegexOption.IGNORE_CASE)),
    CategoryRule(PidCategory.TEMPERATURE, Regex("temp", RegexOption.IGNORE_CASE)),
    CategoryRule(PidCategory.AIR_INTAKE, Regex("\\bair\\b|\\bmaf\\b|flow", RegexOption.IGNORE_CASE)),
    CategoryRule(PidCategory.ELECTRICAL, Regex("voltage|battery|\\bibs\\b|oil level|oil degradation", RegexOption.IGNORE_CASE)),
    CategoryRule(PidCategory.LOCATION, Regex("latitude|longitude|\\blat\\b|\\blon\\b|\\blng\\b|gps", RegexOption.IGNORE_CASE)),
    CategoryRule(PidCategory.BASICS, Regex("speed|\\brpm\\b|pedal|throttle|gear|selector|distance|odometer", RegexOption.IGNORE_CASE))
)

// No upstream category field exists on PidDefinition (nor in the ObdMetrics JSON source), so
// categorization is inferred from the PID's description text, same approach as the web log
// viewer's signal-categories.ts.
fun categoryFor(pid: PidDefinition): PidCategory {
    val text = "${pid.description ?: ""} ${pid.longDescription ?: ""}"
    for (rule in RULES) {
        if (rule.pattern.containsMatchIn(text)) return rule.category
    }
    return PidCategory.OTHER
}

// The grouping a PID header row falls under in the picker list: every checked (selected)
// standard PID is collected into a single Selected group regardless of its category, while
// unchecked ones are grouped by their actual category. Custom (user-added) PIDs have no section.
sealed class PidSection {
    object Selected : PidSection()
    data class Category(val category: PidCategory) : PidSection()
}

fun sectionFor(item: PidDefinitionDetails): PidSection? = when {
    item.source.isUserCustom -> null
    item.checked -> PidSection.Selected
    else -> PidSection.Category(categoryFor(item.source))
}
