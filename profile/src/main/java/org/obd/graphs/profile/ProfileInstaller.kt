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
package org.obd.graphs.profile

import android.util.Log
import org.obd.graphs.getContext
import java.util.Properties

private const val LOG_TAG = "ProfileInstaller"

internal class ProfileInstaller(
    private val repository: ProfileRepository,
) {
    fun install(
        forceOverrideRecommendation: Boolean,
        installationKey: String,
        defaultProfile: String,
    ) {
        val isInstalled = repository.getAll().containsKey(installationKey)
        var forceOverride = forceOverrideRecommendation

        if (!isInstalled){
            forceOverride = true
        }

        if (!isInstalled || forceOverride) {
            Log.i(LOG_TAG, "Starting profile installation. Force=$forceOverride")

            val updates = mutableMapOf<String, Any?>()

            // 1. Load from Assets
            findProfileFiles().forEach { fileName ->
                Log.i(LOG_TAG, "Loading profile file: $fileName")
                val props = loadPropertiesFromAssets(fileName)
                collectProperties(props, updates, forceOverride)
            }

            // 2. Apply Default Profile ID
            if (forceOverride) {
                updates[PROFILE_ID_PREF] = defaultProfile
            }

            // 3. Mark as installed
            updates[installationKey] = true

            // 4. Commit
            if (forceOverride) {
                repository.clear()
            }
            repository.updateBatch(updates)
            Log.i(LOG_TAG, "Profile installation finished.")
        }
    }

    /**
     * Installs settings from a generic Properties object (e.g. from a Backup file).
     */
    fun installFromProperties(
        properties: Properties,
        installationKey: String,
        defaultProfile: String,
    ) {
        Log.i(LOG_TAG, "Installing from properties (Backup Restore)")
        val updates = mutableMapOf<String, Any?>()

        // 1. Collect properties from the file
        collectProperties(properties, updates, forceOverride = true)

        // 2. CRITICAL FIX: Ensure the profile ID is set (reset to default)
        // This matches the original logic which reset the selection after a forced reload.
        updates[PROFILE_ID_PREF] = defaultProfile

        // 3. Ensure installation key is present
        updates[installationKey] = true

        // 4. Clear and Apply
        repository.clear()
        repository.updateBatch(updates)
        Log.i(LOG_TAG, "Restore finished.")
    }

    private fun collectProperties(
        source: Properties,
        updates: MutableMap<String, Any?>,
        forceOverride: Boolean,
    ) {

        source.forEach { (k, v) ->
            val key = k.toString()
            val value = v.toString()
            val parsedValue = parseValue(value)

            if (forceOverride || !repository.getAll().containsKey(key)) {
                updates[key] = parsedValue
            }
        }
    }

    private fun parseValue(value: String): Any =
        when {
            value.isBoolean() -> value.toBoolean()
            value.isNumeric() -> value.toInt()
            value.isArray() ->
                value
                    .removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
            else -> value.replace("\"", "") // Remove quotes from backup strings
        }

    private fun findProfileFiles(): List<String> = getContext()!!.assets.list("")?.filter { it.endsWith("properties") } ?: emptyList()

    private fun loadPropertiesFromAssets(fileName: String): Properties =
        Properties().apply {
            getContext()!!.assets.open(fileName).use { load(it) }
        }
}
