package org.obd.graphs.bl.drag

import android.os.Handler
import android.os.Looper
import org.obd.graphs.MetricsProcessor
import org.obd.graphs.dragracing.BuildConfig
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.query.AMBIENT_TEMP_PID_ID
import org.obd.graphs.query.ATM_PRESSURE_PID_ID
import org.obd.graphs.query.ENGINE_RPM_PID_ID
import org.obd.graphs.query.VEHICLE_SPEED_PID_ID
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


class DragRaceMetricsBroadcaster: MetricsProcessor {

    private val raw = object: ConnectorResponse{
        override fun byteAt(p0: Int): Byte = "".toByte()
        override fun capacity(): Long = 0
        override fun remaining(): Int  = 0
    }

    private lateinit var replyObserver: ReplyObserver<Reply<*>>
    private var broadcasterLaunched:Boolean = false

    private data class Metric (val pid: PidDefinition, val data: MutableList<Int>, var counter: Int=0)
    private val metrics = mutableListOf(
        Metric(
            pid = PidDefinition(ATM_PRESSURE_PID_ID,1,"A","22","18F0","C","Atm Pressure",700,1200, ValueType.INT),
            data = mutableListOf<Int>().apply {
                add(1020)
                add(999)
            }),

        Metric(
            pid = PidDefinition(AMBIENT_TEMP_PID_ID,1,"A","22","18F0","C","Ambient Temp",0,50, ValueType.INT),
            data = mutableListOf<Int>().apply {
                (5..25).forEach{
                    add(it)
                }
            }),
        Metric(
            pid = PidDefinition(VEHICLE_SPEED_PID_ID,1,"A","22","18F0","km/h","Vehicle speed",0,300, ValueType.INT),
            data = mutableListOf<Int>().apply {
                (0..100).forEach{ _ ->
                    add(0)
                }
                (0..300).forEach{
                    add(it)
                }
            }),
        Metric(
            pid = PidDefinition(ENGINE_RPM_PID_ID,1,"A","22","18F0","rpm","Engine speed",0,7100, ValueType.INT),
            data = mutableListOf<Int>().apply {
                for (i in 0..7000 step 10){
                    add(i)
                }
            }))

    override fun onStopped() {
        if (isBroadcastingFakeMetricsEnabled()){
            broadcasterLaunched = false
        }
    }

    override fun postValue(obdMetric: ObdMetric) {
    }

    override fun onRunning(vehicleCapabilities: VehicleCapabilities?) {
        if (isBroadcastingFakeMetricsEnabled()) {
            if (!broadcasterLaunched){
                broadcasterLaunched = true

                val executor: ExecutorService = Executors.newSingleThreadExecutor()
                val handler = Handler(Looper.getMainLooper())

                executor.execute {
                    while (broadcasterLaunched){
                        Thread.sleep(30)
                        metrics.forEach {
                            if (it.counter == it.data.size){
                                it.counter = 0
                            }
                            replyObserver.onNext(ObdMetric.builder()
                                .value(it.data[ it.counter])
                                .raw(raw)
                                .command(ObdCommand(it.pid)).build())
                            it.counter++
                        }
                    }
                    handler.post {}
                }
            }
        }
    }

    override fun init(replyObserver: ReplyObserver<Reply<*>>) {
        this.replyObserver = replyObserver
    }

    private fun isBroadcastingFakeMetricsEnabled(): Boolean = BuildConfig.DEBUG &&
            Prefs.getBoolean("pref.debug.generator.broadcast_fake_metrics", false)
}
