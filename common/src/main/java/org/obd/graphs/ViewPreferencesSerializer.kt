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
package org.obd.graphs

import android.util.Log
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.obd.graphs.preferences.Prefs

internal class ItemPreference(var id: Long, var position: Int)

private const val LOG_TAG = "RecycleViewPreferences"

class ViewPreferencesSerializer constructor(private val prefName: String) {

    private var mapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
    }

    fun getItemsSortOrder(): Map<Long, Int>? = try {
        load()?.associate {
            it.id to it.position
        }
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "Failed to load preferences for $prefName", e)
        null
    }

    fun  store(
        data: List<Long>
    ) {
        try {
            val mapIndexed = data.mapIndexed { index, item ->
                map(item, index)
            }

            val writeValueAsString = mapper.writeValueAsString(mapIndexed)

            Prefs.edit().run {
                putString(prefName, writeValueAsString)
                apply()
            }
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to store preferences for $prefName", e)
        }
    }

    private fun load(): List<ItemPreference>? =
        Prefs.getString(prefName, "")?.let {

            if (Log.isLoggable(LOG_TAG,Log.DEBUG)) {
                Log.d(LOG_TAG, "Loading JSON from prefs=$prefName")
            }

            val listType: CollectionType =
                mapper.typeFactory.constructCollectionType(
                    ArrayList::class.java,
                    ItemPreference::class.java
                )
            return if (it.isEmpty()) listOf() else mapper.readValue(
                it, listType
            )
        }

    private fun  map(m: Long, index: Int): ItemPreference =  ItemPreference(m, index)
}