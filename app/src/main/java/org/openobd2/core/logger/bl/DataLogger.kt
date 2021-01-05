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
    private var pidRegistry: PidRegistry
    private var codecRegistry: CodecRegistry
    private var policy: ProducerPolicy = ProducerPolicy.builder().frequency(50).build()
    private var commandExecutorPolicy: ExecutorPolicy = ExecutorPolicy.builder().frequency(100).build()

    //just a single thread in a pool
    private var executorService: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>(1),
        ThreadPoolExecutor.DiscardPolicy()
    )

    init {
        val source: InputStream = Thread.currentThread().contextClassLoader
            .getResourceAsStream("generic.json")
        pidRegistry = PidRegistry.builder().source(source).build()
        source.close()
        codecRegistry =
            CodecRegistry.builder().evaluateEngine("rhino").pids(pidRegistry).build()

    }

    fun stop() {
        Log.i("DATA_LOGGER_DL", "Stop collecting process")
        buffer.addFirst(QuitCommand()) // quit the CommandExecutor
    }

    fun start(btDeviceName: String, subscriber: CommandReplySubscriber) {
        val task = Runnable {

            Log.i("DATA_LOGGER_DL", "Start collecting process for Device: $btDeviceName")

            val connection = BluetoothConnection(btDeviceName)
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
                .subscribe(producer)
                .subscribe(subscriber)
                .policy(commandExecutorPolicy)
                .codecRegistry(codecRegistry)
                .build()

            buffer.clear()

            val es: ExecutorService = Executors.newFixedThreadPool(2);
            try {
                es.invokeAll(listOf(executor, producer))
            } finally {
                es.shutdown();
                Log.i("DATA_LOGGER_DL", "Collecting process completed")
            }
        }
        executorService.submit(task)
    }
}