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

private class ConnectorResponseSerializer() :
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

private class NopeConnectorResponseSerializer() :
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

class TripModelSerializer {

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