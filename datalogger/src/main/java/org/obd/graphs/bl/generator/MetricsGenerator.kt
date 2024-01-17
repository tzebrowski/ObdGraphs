package org.obd.graphs.bl.generator

import android.os.Handler
import android.os.Looper
import org.obd.graphs.bl.datalogger.MetricsProcessor
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.query.Query
import org.obd.graphs.preferences.Prefs
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.api.model.Reply
import org.obd.metrics.api.model.ReplyObserver
import org.obd.metrics.api.model.VehicleCapabilities
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.ValueType
import org.obd.metrics.transport.message.ConnectorResponse
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MetricsGenerator(private val debugBuild: Boolean) : MetricsProcessor {

    private val raw = object : ConnectorResponse {
        override fun byteAt(p0: Int): Byte = "".toByte()
        override fun capacity(): Long = 0
        override fun remaining(): Int = 0
    }

    private lateinit var replyObserver: ReplyObserver<Reply<*>>
    private var broadcasterLaunched: Boolean = false


    override fun onStopped() {
        if (isGeneratorEnabled()) {
            broadcasterLaunched = false
        }
    }

    override fun postValue(obdMetric: ObdMetric) {
    }

    override fun onRunning(vehicleCapabilities: VehicleCapabilities?) {

        if (isGeneratorEnabled()) {
            val metrics = generateMetricsFor(dataLogger.getCurrentQuery())

            if (!broadcasterLaunched) {
                broadcasterLaunched = true

                val executor: ExecutorService = Executors.newSingleThreadExecutor()
                val handler = Handler(Looper.getMainLooper())

                executor.execute {
                    while (broadcasterLaunched) {
                        Thread.sleep(40)
                        emit(metrics)
                    }
                    handler.post {}
                }
            }
        }
    }

    override fun init(replyObserver: ReplyObserver<Reply<*>>) {
        this.replyObserver = replyObserver
    }

    private fun isGeneratorEnabled(): Boolean = debugBuild &&
            Prefs.getBoolean("pref.debug.generator.broadcast_fake_metrics", false)


    private fun generateMetricsFor(query: Query?) = mutableSetOf<MetricGeneratorDefinition>().apply {
        query?.getIDs()?.forEach { id ->
            if (baseMetrics.containsKey(id)) {
                add(baseMetrics[id]!!)
            } else {
                val pid = dataLogger.getPidDefinitionRegistry().findBy(id)
                val metric = MetricGeneratorDefinition(
                    pid = pid,
                    data = generateSequenceFor(pid)
                )
                add(metric)
            }
        }
    }

    private fun generateSequenceFor(pid: PidDefinition) =
        mutableListOf<Number>().apply {
            if (pid.type == ValueType.DOUBLE) {
                var step = 1.0

                if (pid.max.toInt() < 15) {
                    step = 0.1
                }

                if (pid.max.toInt() > 1000) {
                    step = 5.0
                }

                if (pid.max.toInt() > 5000) {
                    step = 10.0
                }

                (pid.min.toDouble()..pid.max.toDouble() step step).forEach {
                    add(it)
                }
            } else {
                var step = 1

                if (pid.max.toInt() > 1000) {
                    step = 5
                }

                if (pid.max.toInt() > 5000) {
                    step = 10
                }

                (pid.min.toInt()..pid.max.toInt() step step).forEach {
                    add(it)
                }
            }
        }


    private fun emit(entries: Set<MetricGeneratorDefinition>) {
        entries.forEach {
            if (it.counter == it.data.size) {
                it.counter = 0
            }
            replyObserver.onNext(
                ObdMetric.builder()
                    .value(it.data[it.counter])
                    .raw(raw)
                    .command(ObdCommand(it.pid)).build()
            )
            it.counter++
        }
    }

    private infix fun ClosedRange<Double>.step(step: Double): Iterable<Double> {
        require(start.isFinite())
        require(endInclusive.isFinite())
        require(step > 0.0) { "Step must be positive, was: $step." }
        val sequence = generateSequence(start) { previous ->
            if (previous == Double.POSITIVE_INFINITY) return@generateSequence null
            val next = previous + step
            if (next > endInclusive) null else next
        }
        return sequence.asIterable()
    }
}
