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
package org.obd.graphs.bl.datalogger

import android.content.Context
import android.util.Log
import org.obd.graphs.preferences.Prefs
import java.io.File

private const val STORAGE_FILE_CODING_KEY = "storage:"
const val ACCESS_EXTERNAL_STORAGE_ENABLED = "pref.pids.registry.access_external_storage"

val pidResources = PIDsResources()

class PIDsResources {

    internal fun externalResourceToURL(it: String) =
        File(it.substring(STORAGE_FILE_CODING_KEY.length, it.length)).toURI().toURL()

    internal fun isExternalStorageResource(it: String) = it.startsWith(STORAGE_FILE_CODING_KEY)

    fun getDefaultPidFiles(): Map<String,String> =  mapOf(
        "alfa.json" to "Giulietta QV",
        "giulia_2.0_gme.json" to "Giulia 2.0 GME",
        "mode01.json" to "Mode 01",
        "mode01_2.json" to "Mode 01.2",
        "extra.json" to "Extra",
    )

    fun getExternalPidResources(context: Context?): MutableMap<String, String>? {
        return getExternalPidResources(context) {
            Prefs.getBoolean(
                ACCESS_EXTERNAL_STORAGE_ENABLED,
                false
            )
        }
    }

    fun getExternalPidResources(
        context: Context?,
        isFeatureEnabled: () -> Boolean
    ): MutableMap<String, String>? {
        if (isFeatureEnabled()) {
            getExternalPidDirectory(context)?.let { directory ->
                val files = File(directory).listFiles()
                Log.d(
                    LOG_TAG,
                    "Reading directory $directory for available extra PID resource files. " +
                            "\nFound number of files: ${files?.size}"
                )

                return files?.associate {
                    Log.d(
                        LOG_TAG, "Found file: ${it.absolutePath}." +
                                "\n Adding to the path."
                    )
                    "$STORAGE_FILE_CODING_KEY${it.absolutePath}" to it.absolutePath
                }?.toMutableMap()

            }
        }
        return null
    }

    private fun getExternalPidDirectory(context: Context?): String? =
        context?.getExternalFilesDir("pid")?.absolutePath

}