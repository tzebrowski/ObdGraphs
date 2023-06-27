package org.obd.graphs.aa

import android.util.Log
import java.util.concurrent.*


internal class RenderingThread(surfaceController: SurfaceController) {

    private val singleTaskPool: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(1), ThreadPoolExecutor.DiscardPolicy()
    )

    private var tasks: Future<*>? = null

    private val renderingTask: Runnable = Runnable {
        while (!Thread.currentThread().isInterrupted) {
            surfaceController.render()
            Thread.sleep(20)
        }
    }

    fun start() {
        Log.i(LOG_KEY, "Submitting rendering task")
        tasks = singleTaskPool.submit(renderingTask)
        Log.i(LOG_KEY, "Rendering task is submitted")
    }

    fun stop() {
        Log.i(LOG_KEY, "Shutdown rendering task")
        tasks?.cancel(true)
        Log.i(LOG_KEY, "Rendering task is now shutdown")
    }
}