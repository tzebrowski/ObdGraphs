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
@file:Suppress("DEPRECATION")

package org.obd.graphs.bl.datalogger

import android.util.Log
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateString
import org.obd.metrics.pid.PidDefinition

private var mapper = ObjectMapper().apply {
    registerModule(KotlinModule())
    configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
    configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
    configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}


private const val PREF_KEY = "pref.pid.registry.overrides.pid"
private const val TAG = "PID_SER"

fun PidDefinition.serialize(
) = try {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Serialize PID $id=${key()}")
    }
    Prefs.updateString(key(), mapper.writeValueAsString(this)).commit()
} catch (e: Throwable) {
    Log.e(TAG, "Failed to serialize", e)
    null
}

fun PidDefinition.deserialize(): PidDefinition? {
    try {
        val data = Prefs.getString(key(), null)
        return if (data == null) {
            null
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Deserialize $id=${key()}=$data")
            }
            mapper.readValue(data, PidDefinition::class.java)
        }
    } catch (e: Throwable) {
        Log.e(TAG, "Failed to deserialize", e)
        return null
    }
}

private fun PidDefinition.key() = "$PREF_KEY.$id"