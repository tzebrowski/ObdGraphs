package org.obd.graphs

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.util.concurrent.*


private const val LOG_KEY = "RenderingThread"
private const val MSG_RENDER_FRAME = 1

class RenderingThread(renderAction: () -> Unit,private val perfFrameRate: () -> Int) {

    private val singleTaskPool: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(1), ThreadPoolExecutor.DiscardPolicy()
    )

    private val handler = Handler(Looper.getMainLooper(), HandlerCallback(renderAction))
    private var tasks: Future<*>? = null

    internal class HandlerCallback(private val renderAction: () -> Unit) : Handler.Callback {
        override fun handleMessage(msg: Message): Boolean {
            if (msg.what == MSG_RENDER_FRAME) {
                renderAction()
                return true
            }
            return false
        }
    }

    fun isRunning(): Boolean {
        return tasks != null && !tasks!!.isDone
    }

    fun start() {
        Log.d(LOG_KEY, "Submitting rendering task")
        tasks = singleTaskPool.submit(getRenderingTask())
        Log.d(LOG_KEY, "Rendering task is submitted")
    }

    fun stop() {
        Log.d(LOG_KEY, "Shutdown rendering task")
        tasks?.cancel(true)
        handler.removeMessages(MSG_RENDER_FRAME)
        Log.d(LOG_KEY, "Rendering task is now shutdown")
    }

    private fun getRenderingTask(): Runnable = Runnable {
        val fps =  perfFrameRate()
        Log.d(LOG_KEY, "Expected surface FPS $fps")
        val targetDelay = 1000 / fps
        while (!Thread.currentThread().isInterrupted) {
            var ts = System.currentTimeMillis()
            handler.sendEmptyMessage(MSG_RENDER_FRAME)
            ts = System.currentTimeMillis() - ts

            if (targetDelay > ts && (targetDelay - ts) > 0) {
                Thread.sleep(targetDelay - ts)
            }
        }
    }
}