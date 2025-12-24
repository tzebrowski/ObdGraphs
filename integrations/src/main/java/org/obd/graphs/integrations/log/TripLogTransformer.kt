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
package org.obd.graphs.integrations.log

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.File
import java.io.InputStreamReader
import java.io.StringReader

internal interface TripLogTransformer {
    fun transform(log: String): File

    fun transform(file: File): File
}

internal enum class OutputType { JSON }

object TripLog {
    @Suppress("UNUSED_EXPRESSION")
    internal fun transformer(
        outputType: OutputType = OutputType.JSON,
        signalMapper: Map<Int, String> = mapOf(),
        valueMapper: (signal: Int, value: Number) -> Number,
    ): TripLogTransformer =
        when (outputType) {
            else -> DefaultJSONOutput(signalMapper, valueMapper)
        }
}

private class DefaultJSONOutput(
    private val signalMapper: Map<Int, String> = mapOf(),
    private val valueMapper: (signal: Int, value: Number) -> Number,
) : TripLogTransformer {
    override fun transform(file: File): File =
        file.inputStream().use { input ->
            process(JsonReader(InputStreamReader(input)))
        }

    override fun transform(log: String): File = process(JsonReader(StringReader(log)))

    private fun process(reader: JsonReader): File {
        val tempFile = File.createTempFile("json_buffer_", ".tmp").apply {
            // Ensures the file is cleaned up if the JVM shuts down
            deleteOnExit()
        }

        try {
            // Nested .use calls ensure all streams are closed even if an exception occurs
            tempFile.outputStream().bufferedWriter().use { fileWriter ->
                JsonWriter(fileWriter).use { writer ->
                    reader.isLenient = true
                    writer.beginArray()
                    parseRoot(reader, writer)
                    writer.endArray()
                }
            }
            return tempFile
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        } finally {
            try {
                reader.close()
            } catch (ignored: Exception) {
            }
        }
    }


    private fun parseRoot(
        reader: JsonReader,
        writer: JsonWriter,
    ) {
        reader.beginObject() // {
        while (reader.hasNext()) {
            if (reader.nextName() == "entries") {
                parseEntries(reader, writer)
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()
    }

    private fun parseEntries(
        reader: JsonReader,
        writer: JsonWriter,
    ) {
        reader.beginObject() // Start "entries" map
        while (reader.hasNext()) {
            reader.nextName() // Skip the dynamic key ("12", "99")
            parseEntryGroup(reader, writer)
        }
        reader.endObject()
    }

    private fun parseEntryGroup(
        reader: JsonReader,
        writer: JsonWriter,
    ) {
        reader.beginObject() // Inside "12": {
        while (reader.hasNext()) {
            if (reader.nextName() == "metrics") {
                parseMetricsArray(reader, writer)
            } else {
                reader.skipValue() // Skip "id", "mean", etc.
            }
        }
        reader.endObject()
    }

    private fun parseMetricsArray(
        reader: JsonReader,
        writer: JsonWriter,
    ) {
        reader.beginArray() // [
        while (reader.hasNext()) {
            parseSingleMetric(reader, writer)
        }
        reader.endArray()
    }

    private fun parseSingleMetric(
        reader: JsonReader,
        writer: JsonWriter,
    ) {
        var ts: Long = 0
        var signal = 0
        var value = 0.0

        reader.beginObject() // Metric object {
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "ts" -> ts = reader.nextLong()
                "entry" -> {
                    reader.beginObject() // Nested "entry": {
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "data" -> signal = reader.nextInt()
                            "y" -> value = reader.nextDouble()
                            else -> reader.skipValue() // Skip "x"
                        }
                    }
                    reader.endObject()
                }

                else -> reader.skipValue() // Skip "rawAnswer"
            }
        }
        reader.endObject()

        // Write directly to output (No intermediate object creation)
        writer.beginObject()
        writer.name("t").value(ts)
        writer.name("s").value((signalMapper[signal] ?: signal).toString())
        writer.name("v").value(valueMapper(signal, value))
        writer.endObject()
    }
}
