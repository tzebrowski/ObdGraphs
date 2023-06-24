package org.obd.graphs.aa

import android.util.Log
import java.util.concurrent.*

internal class CarRenderingThread ( surfaceController: SurfaceController) {

    private val singleTaskPool: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue<Runnable>(1), ThreadPoolExecutor.DiscardPolicy()
    )

    private var tasks: Future<*>? = null

    private val renderingTask: Runnable = Runnable {
        while (!Thread.currentThread().isInterrupted) {
            surfaceController.render()
            Thread.sleep(5)
        }
    }

    fun start () {
        Log.e(LOG_KEY, "Submitting rendering task")
        tasks = singleTaskPool.submit(renderingTask)
        Log.e(LOG_KEY, "Rendering task is submitted")
    }

    fun stop () {
        Log.e(LOG_KEY, "Shutdown down rendering task")
        tasks?.cancel(true)
        Log.e(LOG_KEY, "Rendering task is now shutdown down")
    }
}