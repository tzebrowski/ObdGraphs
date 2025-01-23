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
package org.obd.graphs.bl.trip

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.obd.graphs.preferences.Prefs
import org.obd.metrics.transport.message.ConnectorResponse
import org.obd.metrics.transport.message.ConnectorResponseFactory
import java.io.IOException

private val EMPTY_CONNECTOR_RESPONSE = ConnectorResponseFactory.wrap(byteArrayOf())

private class ConnectorResponseSerializer:
    StdSerializer<ConnectorResponse>(ConnectorResponse::class.java) {

    @Throws(IOException::class)
    override fun serialize(
        value: ConnectorResponse,
        gen: JsonGenerator,
        provider: SerializerProvider
    ) {
        gen.writeString(value.message)
    }
}

private class NopeConnectorResponseSerializer:
    StdSerializer<ConnectorResponse>(ConnectorResponse::class.java) {

    @Throws(IOException::class)
    override fun serialize(
        value: ConnectorResponse,
        gen: JsonGenerator,
        provider: SerializerProvider
    ) {
        gen.writeString("")
    }
}

private class ConnectorResponseDeserializer() :
    StdDeserializer<ConnectorResponse>(String::class.java) {

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): ConnectorResponse {
        return EMPTY_CONNECTOR_RESPONSE
    }
}

internal class TripModelSerializer {

    val serializer: ObjectMapper by lazy { serializer() }
    val deserializer: ObjectMapper by lazy { deserializer() }

    private fun serializer(): ObjectMapper =
        jacksonObjectMapper().apply {
            val module = SimpleModule()
            val serializeConnectorResponse =
                Prefs.getBoolean("pref.debug.trip.save.connector_response", false)
            if (serializeConnectorResponse) {
                module.addSerializer(
                    ConnectorResponse::class.java,
                    ConnectorResponseSerializer()
                )
            } else {
                module.addSerializer(
                    ConnectorResponse::class.java,
                    NopeConnectorResponseSerializer()
                )
            }

            module.addDeserializer(
                ConnectorResponse::class.java,
                ConnectorResponseDeserializer()
            )
            registerModule(module)
        }

    private fun deserializer(): ObjectMapper =
        jacksonObjectMapper().apply {
            val module = SimpleModule()
            module.addSerializer(
                ConnectorResponse::class.java,
                ConnectorResponseSerializer()
            )
            module.addDeserializer(
                ConnectorResponse::class.java,
                ConnectorResponseDeserializer()
            )
            registerModule(module)
        }
}