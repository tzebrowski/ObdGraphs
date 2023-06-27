package org.obd.graphs.aa

import android.util.Log
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import java.util.concurrent.*


internal class RenderingThread(private val surfaceController: SurfaceController) {

    private val singleTaskPool: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(1), ThreadPoolExecutor.DiscardPolicy()
    )

    private var tasks: Future<*>? = null


    fun start() {
        Log.e(LOG_KEY, "Submitting rendering task")
        tasks = singleTaskPool.submit(getRenderingTask(surfaceController))
        Log.i(LOG_KEY, "Rendering task is submitted")
    }

    fun stop() {
        Log.i(LOG_KEY, "Shutdown rendering task")
        tasks?.cancel(true)
        Log.i(LOG_KEY, "Rendering task is now shutdown")
    }

    private fun getRenderingTask(surfaceController: SurfaceController): Runnable  = Runnable {
        val fps = Prefs.getS("pref.aa.surface.fps", "20").toInt()
        Log.i(LOG_KEY, "Expected surface FPS $fps")
        val targetDelay = 1000 / fps
        while (!Thread.currentThread().isInterrupted) {
            var ts = System.currentTimeMillis()
            surfaceController.render()
            ts = System.currentTimeMillis() - ts

            if (targetDelay > ts) {
                val wait = targetDelay - ts
                Thread.sleep(wait)
            }
        }
    }

}