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
package org.obd.graphs.preferences.dtc

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import org.obd.graphs.profile.profile
import java.net.URLEncoder

internal const val PREF_VEHICLE_MAKE = "pref.vehicle_settings.make"
internal const val PREF_VEHICLE_MODEL = "pref.vehicle_settings.model"
internal const val PREF_VEHICLE_ENGINE = "pref.vehicle_settings.engine"

// udm=50 opens Google's AI Mode; where it is not offered Google shows ordinary results for the same text.
private const val GOOGLE_AI_SEARCH_URL = "https://www.google.com/search?udm=50&q="

internal data class DtcPromptLabels(
    val code: String,
    val codeWithDescription: String,
    val vehicle: String,
    val module: String,
    val status: String,
    val question: String
)

// "Alfa Romeo Giulia 2.0 GME" from whichever of the three vehicle settings are filled in; null when none is.
internal fun vehicleDescription(
    make: String?,
    model: String?,
    engine: String?
): String? = listOf(make, model, engine).mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }.joinToString(" ").ifEmpty { null }

// A question for the AI, not a keyword query. Parts that are unknown are left out. String resources drop a
// leading space, so the parts are joined here.
internal fun dtcAiPrompt(
    labels: DtcPromptLabels,
    code: String,
    description: String?,
    failureType: String?,
    vehicle: String?,
    module: String?,
    status: String?
): String {
    val described = description?.trim()?.trimEnd('.')?.takeIf { it.isNotEmpty() }
    return listOfNotNull(
        if (described != null) labels.codeWithDescription.format(code, described) else labels.code.format(code),
        failureType?.trim()?.trimEnd('.')?.takeIf { it.isNotEmpty() },
        vehicle?.let { labels.vehicle.format(it) },
        module?.let { labels.module.format(it) },
        status?.let { labels.status.format(it) },
        labels.question
    ).joinToString(". ")
}

internal fun dtcAiSearchUrl(prompt: String): String = GOOGLE_AI_SEARCH_URL + URLEncoder.encode(prompt, "UTF-8")

// The vehicle from the settings. A key never set falls back to the current profile's preset: an upgrade adds the
// preset to the profile only, and it reaches the active keys when the profile is next loaded.
internal fun configuredVehicle(): String? {
    fun value(key: String) =
        if (Prefs.contains(key)) Prefs.getS(key, "") else Prefs.getS("${profile.getCurrentProfile()}.$key", "")
    return vehicleDescription(value(PREF_VEHICLE_MAKE), value(PREF_VEHICLE_MODEL), value(PREF_VEHICLE_ENGINE))
}
