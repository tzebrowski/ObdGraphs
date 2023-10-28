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
package org.obd.graphs.ui.recycler

import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.obd.graphs.bl.collector.CarMetric
import org.obd.graphs.preferences.Prefs
import org.obd.metrics.api.model.ObdMetric

internal class ItemPreference(var id: Long, var position: Int)

internal class RecycleViewPreferences constructor(private val prefName: String) {
    private var mapper = ObjectMapper()

    init {
        mapper.registerModule(KotlinModule())
    }

    internal fun getItemsSortOrder(): Map<Long, Int>? = try {
        load()?.associate {
            it.id to it.position
        }
    } catch (e: Throwable){
        Log.e("RecycleViewPreferences","Failed to parse property $prefName",e)
        null
    }

    internal fun store(
        data: MutableList<CarMetric>
    ) {

        val mapIndexed = data.mapIndexed { index, metric ->
            map(metric.source, index)
        }

        val writeValueAsString = mapper.writeValueAsString(mapIndexed)

        Prefs.edit().run {
            putString(prefName, writeValueAsString)
            apply()
        }
    }

    private fun load(): List<ItemPreference>? {
        val it = Prefs.getString(prefName, "")
        val listType: CollectionType =
            mapper.typeFactory.constructCollectionType(
                ArrayList::class.java,
                ItemPreference::class.java
            )

        return if (it!!.isEmpty()) listOf() else mapper.readValue(
            it, listType
        )
    }

    private fun map(m: ObdMetric, index: Int): ItemPreference {
        return ItemPreference(m.command.pid.id, index)
    }
}