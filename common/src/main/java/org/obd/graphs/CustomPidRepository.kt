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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.obd.metrics.pid.PidDefinition
import java.io.File

const val USER_CUSTOM_PIDS_FILE = "user_custom_pids.json"

class CustomPidRepository(private val context: Context) {

    private val logTag = "CustomPidRepository"

    private val mapper = ObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    private fun getFile(): File = File(context.filesDir, USER_CUSTOM_PIDS_FILE)

    suspend fun saveCustomPid(pidDefinition: PidDefinition) = withContext(Dispatchers.IO) {
        val file = getFile()
        val typeRef = object : TypeReference<MutableMap<String, MutableList<PidDefinition>>>() {}

        val pidsData = if (file.exists()) {
            try {
                mapper.readValue(file, typeRef)
            } catch (e: Exception) {
                Log.e(logTag, "Failed to read existing custom PIDs.", e)
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
            Log.i(logTag, "Successfully saved custom PID (ID: ${pidDefinition.id})")
        } catch (e: Exception) {
            Log.e(logTag, "Failed to write custom PIDs to file", e)
        }
    }

    suspend fun deleteCustomPid(pidId: Long) = withContext(Dispatchers.IO) {
        val file = getFile()
        if (!file.exists()) return@withContext

        val typeRef = object : TypeReference<MutableMap<String, MutableList<PidDefinition>>>() {}

        try {
            val pidsData = mapper.readValue(file, typeRef)
            val livedata = pidsData["livedata"] ?: return@withContext

            if (livedata.removeAll { it.id == pidId }) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, pidsData)
                Log.i(logTag, "Successfully deleted PID (ID: $pidId)")
            }
        } catch (e: Exception) {
            Log.e(logTag, "Error occurred while removing custom PID", e)
        }
    }
}
