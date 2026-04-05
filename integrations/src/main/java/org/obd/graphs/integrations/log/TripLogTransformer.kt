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
package org.obd.graphs.integrations.log

import android.util.Log
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.EOFException
import java.io.File
import java.io.InputStreamReader
import java.io.StringReader

internal interface TripLogTransformer {
    fun transform(log: String, metadata: Map<String, String> = mapOf()): File
    fun transform(file: File, metadata: Map<String, String> = mapOf()): File
}

internal enum class OutputType { JSON }

object TripLog {
    internal fun transformer(
        outputType: OutputType = OutputType.JSON,
        signalMapper: Map<Int, String> = mapOf(),
        valueMapper: (signal: Int, value: Any) -> Any
    ): TripLogTransformer =
        when (outputType) {
            else -> DefaultJSONOutput(signalMapper, valueMapper)
        }
}

private const val LOG_TAG = "DefaultJSONOutput"

private class DefaultJSONOutput(
    private val signalMapper: Map<Int, String> = mapOf(),
    private val valueMapper: (signal: Int, value: Any) -> Any
) : TripLogTransformer {

    private class SeriesData {
        val timestamps = mutableListOf<Long>()
        val values = mutableListOf<Any?>()
    }

    override fun transform(file: File, metadata: Map<String, String>): File =
        file.inputStream().use { input ->
            if (Log.isLoggable(LOG_TAG,Log.DEBUG)) {
                Log.d(LOG_TAG, "Received file for transformation name=${file.name}, length=${file.length()}, metadata=$metadata")
            }

            process(JsonReader(InputStreamReader(input)), metadata)
        }

    override fun transform(log: String, metadata: Map<String, String>): File =
        process(JsonReader(StringReader(log)), metadata)

    private fun process(reader: JsonReader, metadata: Map<String, String>): File {

        val tempFile =
            File.createTempFile("json_buffer_", ".tmp").apply {
                deleteOnExit()
            }

        val seriesMap = mutableMapOf<String, SeriesData>()

        try {
            reader.isLenient = true

            // Hybrid parsing loop supports both Legacy JSON and New JSONL
            try {
                while (reader.peek() != JsonToken.END_DOCUMENT) {
                    if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                        reader.beginObject()

                        var ts: Long = 0
                        var signal = 0
                        var value: Any = 0.0
                        var isFlatMetric = false

                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                // LEGACY FORMAT ROUTES
                                "startTs" -> reader.skipValue()
                                "entries" -> parseEntriesToMemory(reader, seriesMap)

                                // NEW JSONL FORMAT ROUTES
                                "ts" -> {
                                    isFlatMetric = true
                                    ts = reader.nextLong()
                                }
                                "entry" -> {
                                    isFlatMetric = true
                                    reader.beginObject()
                                    while (reader.hasNext()) {
                                        when (reader.nextName()) {
                                            "data" -> signal = reader.nextInt()
                                            "y" -> {
                                                value = if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                                                    reader.readMap()
                                                } else {
                                                    reader.nextDouble()
                                                }
                                            }
                                            else -> reader.skipValue()
                                        }
                                    }
                                    reader.endObject()
                                }
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()

                        if (isFlatMetric) {
                            val signalKey = signal.toString()
                            val mappedResult = valueMapper(signal, value)

                            val series = seriesMap.getOrPut(signalKey) { SeriesData() }
                            series.timestamps.add(ts)
                            series.values.add(mappedResult)
                        }
                    } else {
                        reader.skipValue()
                    }
                }
            } catch (e: EOFException) {
                // Safely reached the end of the stream
            }

            tempFile.outputStream().bufferedWriter().use { fileWriter ->
                JsonWriter(fileWriter).use { writer ->
                    writer.beginObject() // Root object

                    // Write Metadata
                    if (metadata.isNotEmpty()) {
                        writer.name("metadata")
                        writer.beginObject()
                        metadata.forEach { (key, value) ->
                            writer.name(key).value(value)
                        }
                        writer.endObject()
                    }

                    // Write Signal Dictionary
                    writer.name("signal_dictionary")
                    writer.beginObject()
                    seriesMap.keys.forEach { signalKey ->
                        val idAsInt = signalKey.toIntOrNull()
                        val translatedName = if (idAsInt != null) {
                            signalMapper[idAsInt] ?: signalKey
                        } else {
                            signalKey
                        }
                        writer.name(signalKey).value(translatedName.toString())
                    }
                    writer.endObject()

                    // Write Series Data
                    writer.name("series")
                    writer.beginObject()
                    seriesMap.forEach { (signalId, seriesData) ->
                        writer.name(signalId)
                        writer.beginObject()

                        writer.name("t")
                        writer.beginArray()
                        seriesData.timestamps.forEach { writer.value(it) }
                        writer.endArray()

                        writer.name("v")
                        writer.beginArray()
                        seriesData.values.forEach { writer.writeDynamicValue(it) }
                        writer.endArray()

                        writer.endObject()
                    }
                    writer.endObject() // end series
                    writer.endObject() // end root
                }
            }
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            throw e
        } finally {
            try {
                reader.close()
            } catch (ignored: Exception) {
            }
        }
    }

    // ==========================================
    // LEGACY FORMAT PARSING HELPERS
    // ==========================================

    private fun parseEntriesToMemory(
        reader: JsonReader,
        seriesMap: MutableMap<String, SeriesData>
    ) {
        reader.beginObject()
        while (reader.hasNext()) {
            reader.nextName() // Skip the dynamic key ("12", "99")
            parseEntryGroupToMemory(reader, seriesMap)
        }
        reader.endObject()
    }

    private fun parseEntryGroupToMemory(
        reader: JsonReader,
        seriesMap: MutableMap<String, SeriesData>
    ) {
        reader.beginObject()
        while (reader.hasNext()) {
            if (reader.nextName() == "metrics") {
                parseMetricsArrayToMemory(reader, seriesMap)
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()
    }

    private fun parseMetricsArrayToMemory(
        reader: JsonReader,
        seriesMap: MutableMap<String, SeriesData>
    ) {
        reader.beginArray()
        while (reader.hasNext()) {
            parseSingleMetricToMemory(reader, seriesMap)
        }
        reader.endArray()
    }

    private fun parseSingleMetricToMemory(
        reader: JsonReader,
        seriesMap: MutableMap<String, SeriesData>
    ) {
        var ts: Long = 0
        var signal = 0
        var value: Any = 0.0

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "ts" -> ts = reader.nextLong()
                "entry" -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "data" -> signal = reader.nextInt()
                            "y" -> {
                                value = if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                                    reader.readMap()
                                } else {
                                    reader.nextDouble()
                                }
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val signalKey = signal.toString() // Group purely by ID
        val mappedResult = valueMapper(signal, value)

        val series = seriesMap.getOrPut(signalKey) { SeriesData() }
        series.timestamps.add(ts)
        series.values.add(mappedResult)
    }

    private fun JsonReader.readMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()

        this.beginObject()
        while (this.hasNext()) {
            val key = this.nextName()
            val value: Any? = when (this.peek()) {
                JsonToken.BEGIN_OBJECT -> readMap()
                JsonToken.BEGIN_ARRAY -> {
                    this.skipValue()
                    null
                }
                JsonToken.NUMBER -> this.nextDouble()
                JsonToken.STRING -> this.nextString()
                JsonToken.BOOLEAN -> this.nextBoolean()
                else -> {
                    this.skipValue()
                    null
                }
            }
            map[key] = value
        }
        this.endObject()

        return map
    }

    private fun JsonWriter.writeDynamicValue(value: Any?) {
        when (value) {
            null -> this.nullValue()
            is Number -> this.value(value)
            is String -> this.value(value)
            is Boolean -> this.value(value)
            is Map<*, *> -> {
                this.beginObject()
                for ((k, v) in value) {
                    this.name(k.toString())
                    writeDynamicValue(v)
                }
                this.endObject()
            }
            is Collection<*> -> {
                this.beginArray()
                for (item in value) {
                    writeDynamicValue(item)
                }
                this.endArray()
            }
            else -> this.value(value.toString())
        }
    }
}
