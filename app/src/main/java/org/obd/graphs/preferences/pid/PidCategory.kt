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

enum class PidCategory(val stringRes: Int, val shortStringRes: Int) {
    BASICS(R.string.pref_pid_manage_dialog_category_basics, R.string.pref_pid_manage_dialog_category_basics_short),
    IGNITION(R.string.pref_pid_manage_dialog_category_ignition, R.string.pref_pid_manage_dialog_category_ignition_short),
    FUEL_AFR(R.string.pref_pid_manage_dialog_category_fuel_afr, R.string.pref_pid_manage_dialog_category_fuel_afr_short),
    BOOST(R.string.pref_pid_manage_dialog_category_boost, R.string.pref_pid_manage_dialog_category_boost_short),
    LOAD_TORQUE(R.string.pref_pid_manage_dialog_category_load_torque, R.string.pref_pid_manage_dialog_category_load_torque_short),
    TEMPERATURE(R.string.pref_pid_manage_dialog_category_temperature, R.string.pref_pid_manage_dialog_category_temperature_short),
    AIR_INTAKE(R.string.pref_pid_manage_dialog_category_air_intake, R.string.pref_pid_manage_dialog_category_air_intake_short),
    LOCATION(R.string.pref_pid_manage_dialog_category_location, R.string.pref_pid_manage_dialog_category_location_short),
    IBS(R.string.pref_pid_manage_dialog_category_ibs, R.string.pref_pid_manage_dialog_category_ibs_short),
    OIL(R.string.pref_pid_manage_dialog_category_oil, R.string.pref_pid_manage_dialog_category_oil_short),
    OTHER(R.string.pref_pid_manage_dialog_category_other, R.string.pref_pid_manage_dialog_category_other_short)
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
    PidCategory.LOCATION,
    PidCategory.IBS,
    PidCategory.OIL,
    PidCategory.OTHER
)

private data class CategoryRule(val category: PidCategory, val pattern: Regex)

// Ordered most-specific first, e.g. "O2 Voltage" must match FUEL_AFR before the generic
// "voltage" keyword in IBS, and "Calculated horse power" must match LOAD_TORQUE.
private val RULES: List<CategoryRule> = listOf(
    // Checked first: IAM/IBS readings also mention words like "battery" and "boost status" that
    // would otherwise be claimed by the BOOST rule below.
    CategoryRule(PidCategory.IBS, Regex("\\biam\\b|\\bibs\\b|voltage|battery", RegexOption.IGNORE_CASE)),
    CategoryRule(PidCategory.IGNITION, Regex("spark|ignition|timing adv|misfire|knock|coil|injection", RegexOption.IGNORE_CASE)),
    // Word-bounded so it doesn't match inside "coil" (already claimed by IGNITION above).
    CategoryRule(PidCategory.OIL, Regex("\\boil\\b", RegexOption.IGNORE_CASE)),
    CategoryRule(PidCategory.FUEL_AFR, Regex("\\bafr\\b|lambda|fuel|air.?fuel|\\bo2\\b|oxygen", RegexOption.IGNORE_CASE)),
    CategoryRule(
        PidCategory.BOOST,
        Regex("boost|wastegate|turbo|manifold|intake pressure|\\bmap\\b|atmospheric|baro|throttle position|throttle angle", RegexOption.IGNORE_CASE)
    ),
    CategoryRule(PidCategory.LOAD_TORQUE, Regex("torque|\\bload\\b|horsepower|horse power|\\bhp\\b", RegexOption.IGNORE_CASE)),
    CategoryRule(PidCategory.TEMPERATURE, Regex("temp", RegexOption.IGNORE_CASE)),
    CategoryRule(PidCategory.AIR_INTAKE, Regex("\\bair\\b|\\bmaf\\b|flow", RegexOption.IGNORE_CASE)),
    CategoryRule(PidCategory.LOCATION, Regex("latitude|longitude|\\blat\\b|\\blon\\b|\\blng\\b|gps", RegexOption.IGNORE_CASE)),
    CategoryRule(PidCategory.BASICS, Regex("speed|\\brpm\\b|pedal|throttle|gear|selector|distance|odometer", RegexOption.IGNORE_CASE))
)

// No upstream category field exists on PidDefinition (nor in the ObdMetrics JSON source), so
// categorization is inferred from the PID's description text, same approach as the web log
// viewer's signal-categories.ts.
fun categoryFor(pid: PidDefinition): PidCategory {
    // Descriptions in the PID JSON sources often wrap onto multiple lines (e.g. "Measured
    // Intake\nPressure"), which would otherwise break multi-word keywords like "intake pressure".
    val text = "${pid.description ?: ""} ${pid.longDescription ?: ""}".replace('\n', ' ')
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
