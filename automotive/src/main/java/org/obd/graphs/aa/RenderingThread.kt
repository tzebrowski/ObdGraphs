package org.obd.graphs.aa

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.util.concurrent.*

private const val MSG_RENDER_FRAME = 1

internal class RenderingThread(surfaceController: SurfaceController) {

    private val singleTaskPool: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(1), ThreadPoolExecutor.DiscardPolicy()
    )

    private val handler = Handler(Looper.getMainLooper(), HandlerCallback(surfaceController))

    private var tasks: Future<*>? = null

    internal class HandlerCallback(private val surfaceController: SurfaceController) : Handler.Callback {
        override fun handleMessage(msg: Message): Boolean {
            if (msg.what == MSG_RENDER_FRAME) {
                surfaceController.renderFrame()
                return true
            }
            return false
        }
    }

    fun isRunning(): Boolean {
        return tasks != null && !tasks!!.isDone
    }

    fun start() {
        Log.i(LOG_KEY, "Submitting rendering task")
        tasks = singleTaskPool.submit(getRenderingTask())
        Log.i(LOG_KEY, "Rendering task is submitted")
    }

    fun stop() {
        Log.i(LOG_KEY, "Shutdown rendering task")
        tasks?.cancel(true)
        handler.removeMessages(MSG_RENDER_FRAME)
        Log.i(LOG_KEY, "Rendering task is now shutdown")
    }

    private fun getRenderingTask(): Runnable  = Runnable {
        val fps = carSettings.getSurfaceFrameRate()
        Log.i(LOG_KEY, "Expected surface FPS $fps")
        val targetDelay = 1000 / fps
        while (!Thread.currentThread().isInterrupted) {
            var ts = System.currentTimeMillis()
            handler.sendEmptyMessage(MSG_RENDER_FRAME)
            ts = System.currentTimeMillis() - ts

            if (targetDelay > ts && (targetDelay - ts) > 0) {
                Thread.sleep( targetDelay - ts)
            }
        }
    }
}