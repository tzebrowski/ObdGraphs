package org.obd.graphs

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.util.*
import java.util.concurrent.*


private const val LOG_KEY = "RenderingThread"
private const val MSG_RENDER_FRAME = 1

class RenderingThread(private val id: String = UUID.randomUUID().toString(), renderAction: () -> Unit, private val perfFrameRate: () -> Int) {

    private val singleTaskPool: ExecutorService = ThreadPoolExecutor(1, 1, 1L, TimeUnit.SECONDS, SynchronousQueue())

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

    fun isRunning():Boolean = tasks != null && !tasks!!.isDone

    fun start() {
        try {
            Log.i(LOG_KEY, "[${id}] Submitting rendering task")
            tasks = singleTaskPool.submit(getRenderingTask())
            Log.i(LOG_KEY, "[${id}] Rendering task is submitted")
        }catch (e: RejectedExecutionException){
            Log.w(LOG_KEY, "[${id}] Task was rejected. Something is already running.")
        }
    }

    fun stop() {
        Log.i(LOG_KEY, "[${id}] Shutdown rendering task")
        val res = tasks?.cancel(true)
        handler.removeMessages(MSG_RENDER_FRAME)
        Log.i(LOG_KEY, "[${id}] Rendering task is now shutdown, result=$res")
    }

    private fun getRenderingTask(): Runnable = Runnable {
        val fps =  perfFrameRate()
        Log.d(LOG_KEY, "[${id}] Expected surface FPS $fps")
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