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
package org.obd.graphs

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.obd.graphs.preferences.Prefs
import org.obd.metrics.pid.PidDefinition
import java.io.File
import java.net.URL
import java.util.*

const val MODULES_LIST_CHANGED_EVENT = "data.logger.resources.changed.event"
const val ACCESS_EXTERNAL_STORAGE_ENABLED = "pref.pids.registry.access_external_storage"

private const val STORAGE_FILE_CODING_KEY = "storage:"
private const val LOG_TAG = "Modules"
const val USER_CUSTOM_PIDS_FILE = "user_custom_pids.json"

val modules = Modules()

class Modules {

    private val overrides = mapOf(
        "alfa.json" to "Giulietta QV",
        "giulia_2.0_gme.json" to "Giulia 2.0 GME",
        USER_CUSTOM_PIDS_FILE to "User custom PIDs"
    )

    private val mapper = ObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    private var modules = mutableMapOf<String, String>()

    fun externalModuleToURL(it: String): URL = File(it.substring(STORAGE_FILE_CODING_KEY.length, it.length)).toURI().toURL()

    fun isExternalStorageModule(it: String) = it.startsWith(STORAGE_FILE_CODING_KEY)

    fun getDefaultModules(): Map<String, String> = modules.apply {
        putAll(overrides)
    }

    fun deleteCustomPid(context: Context, pidId: Long) {
        getCustomPidsFile(context)?.let { file ->
            if (!file.exists()) return

            val mapper = ObjectMapper()
            val typeRef = object : TypeReference<MutableMap<String, MutableList<PidDefinition>>>() {}

            try {
                val pidsData = mapper.readValue(file, typeRef)
                val livedata = pidsData["livedata"] ?: return

                val initialSize = livedata.size
                livedata.removeAll { it.id == pidId }

                if (livedata.size < initialSize) {
                    mapper.writerWithDefaultPrettyPrinter().writeValue(file, pidsData)
                    Log.i(LOG_TAG, "Successfully deleted PID (ID: $pidId)")
                } else {
                    //
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error occurred while removing custom PID", e)
            }
        }
    }

    fun saveCustomPid(context: Context, pidDefinition: PidDefinition) {
        getCustomPidsFile(context)?.let { file ->

            val typeRef = object : TypeReference<MutableMap<String, MutableList<PidDefinition>>>() {}

            val pidsData: MutableMap<String, MutableList<PidDefinition>> = if (file.exists()) {
                try {
                    mapper.readValue(file, typeRef)
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Failed to read existing custom PIDs. Creating a new wrapper.", e)
                    mutableMapOf("livedata" to mutableListOf())
                }
            } else {
                mutableMapOf("livedata" to mutableListOf())
            }

            val livedata = pidsData.getOrPut("livedata") { mutableListOf() }

            pidDefinition.resourceFile = USER_CUSTOM_PIDS_FILE

            val index = livedata.indexOfFirst { it.id == pidDefinition.id }
            if (index >= 0) {
                livedata[index] = pidDefinition
            } else {
                livedata.add(pidDefinition)
            }

            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, pidsData)
                Log.i(LOG_TAG, "Successfully saved custom PID (ID: ${pidDefinition.id}) to ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to write custom PIDs to file", e)
            }
        }
    }

    fun getCustomPidsFile(context: Context? = getContext()): File? =
        context?.let {
            return File(context.filesDir, USER_CUSTOM_PIDS_FILE)
        }

    fun updateSettings(allProps: MutableMap<String, Any?>) {
        val keys = allProps.keys.filter { it.contains(PREF_MODULE_LIST) }.toList()
        val values = keys.map { allProps[it].toString().replace("[", "").replace("]", "").split(",") }.flatten().toSet()
        val resourcesMap = values.map {
            it.replace(" ", "") to
                it.replace(".json", "")
                    .replace("_", " ")
                    .trim()
                    .split(" ").joinToString(" ") {
                        it.lowercase(Locale.getDefault())
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    }
        }

        modules.putAll(resourcesMap)
        Log.i(LOG_TAG, "Registered following resource modules files: $modules")
    }

    fun getExternalModules(context: Context?): MutableMap<String, String>? = getExternalModules(context) {
        Prefs.getBoolean(
            ACCESS_EXTERNAL_STORAGE_ENABLED,
            false
        )
    }

    fun getExternalModules(
        context: Context?,
        isFeatureEnabled: () -> Boolean
    ): MutableMap<String, String>? = if (isFeatureEnabled()) {
        getExternalModulesDirectory(context)?.let { directory ->
            val files = File(directory).listFiles()
            Log.d(
                LOG_TAG,
                "Reading directory $directory for available extra PID resource files. " +
                    "\nFound number of files: ${files?.size}"
            )

            return files?.associate {
                Log.d(
                    LOG_TAG,
                    "Found file: ${it.absolutePath}." +
                        "\n Adding to the path."
                )
                "$STORAGE_FILE_CODING_KEY${it.absolutePath}" to it.absolutePath
            }?.toMutableMap()
        }
    } else {
        null
    }

    private fun getExternalModulesDirectory(context: Context?): String? =
        context?.getExternalFilesDir("pid")?.absolutePath
}
