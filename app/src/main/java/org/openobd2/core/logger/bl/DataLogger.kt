package org.openobd2.core.logger.bl

import android.util.Log
import org.openobd2.core.*
import org.openobd2.core.codec.CodecRegistry
import org.openobd2.core.command.process.QuitCommand
import org.openobd2.core.pid.PidRegistry
import java.io.InputStream
import java.util.concurrent.*

internal class DataLogger {
    private var buffer: CommandsBuffer = CommandsBuffer.instance()

    //just a single thread in a pool
    private var executorService: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>(1),
        ThreadPoolExecutor.DiscardPolicy()
    )

    fun stop() {
        Log.i("DATA_LOGGER_DL", "Stop collecting process")
        buffer.addFirst(QuitCommand()) // quit the CommandExecutor
    }

    fun start(btDeviceName: String,replySubscriber: CommandReplySubscriber) {

        val func = Runnable {

            Log.i("DATA_LOGGER_DL", "Start collecting process for Device: $btDeviceName")

            val source: InputStream = Thread.currentThread().contextClassLoader
                .getResourceAsStream("generic.json")

            var pidRegistry: PidRegistry = PidRegistry.builder().source(source).build()
            source.close()

            val connection = BluetoothConnection()
            connection.initBluetooth(btDeviceName)

            val collector = DataCollector()

            val codecRegistry: CodecRegistry =
                CodecRegistry.builder().evaluateEngine("rhino").pids(pidRegistry).build()


            val policy: ProducerPolicy = ProducerPolicy.builder().frequency(50).build()
            val producer: Mode1CommandsProducer = Mode1CommandsProducer
                .builder()
                .buffer(buffer)
                .pidDefinitionRegistry(pidRegistry)
                .policy(policy)
                .build()

            val executor: CommandExecutor = CommandExecutor
                .builder()
                .connection(connection)
                .buffer(buffer)
                .subscribe(collector)
                .subscribe(producer)
                .subscribe(replySubscriber)
                .policy(ExecutorPolicy.builder().frequency(100).build())
                .codecRegistry(codecRegistry)
                .build()

            buffer.clear()

            val es: ExecutorService = Executors.newFixedThreadPool(2);
            try {
                es.invokeAll(listOf(executor, producer))
            } finally {
                Log.i("DATA_LOGGER_DL", "Collecting process completed")
                es.shutdown();
            }
        }
        executorService.submit(func)
    }
}