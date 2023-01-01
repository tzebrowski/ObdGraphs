package org.obd.graphs.bl.trip

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.mikephil.charting.data.Entry
import org.obd.metrics.transport.message.ConnectorResponse

data class TripFileDesc(
    val fileName: String,
    val profileId: String,
    val profileLabel: String,
    val startTime: String,
    val tripTimeSec: String
)

data class Metric(
    val entry: Entry,
    val ts: Long,
    val rawAnswer: ConnectorResponse
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SensorData(
    val id: Long,
    val metrics: MutableList<Metric>,
    var min: Number = 0,
    var max: Number = 0,
    var mean: Number = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorData

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Trip(val startTs: Long, val entries: MutableMap<Long, SensorData>)
