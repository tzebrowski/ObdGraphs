package org.obd.graphs.aa

import android.util.Log
import java.util.concurrent.*

private const val TAG_KEY = "CarRenderingThread"
internal class CarRenderingThread ( surfaceController: SurfaceController) {

    private val singleTaskPool: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue<Runnable>(1), ThreadPoolExecutor.DiscardPolicy()
    )

    private var tasks: Future<*>? = null

    private val renderingTask: Runnable = Runnable {

        while (!Thread.currentThread().isInterrupted) {
            surfaceController.render()
            try {
                Thread.sleep(10)
            } catch (e: InterruptedException) {}
        }
    }

    fun start () {
        Log.e(TAG_KEY, "Submitting rendering task")
        tasks = singleTaskPool.submit(renderingTask)
        Log.e(TAG_KEY, "Rendering task is submitted")
    }

    fun stop () {
        Log.e(TAG_KEY, "Shutdown down rendering task")
        tasks?.cancel(true)
        Log.e(TAG_KEY, "Rendering task is now shutdown down")
    }
}