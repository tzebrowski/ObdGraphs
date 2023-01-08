package org.obd.graphs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CountDownLatchBroadcastReceiver (val broadcastEvent:String, private val timeout: Long = 5) {
    val eventGate  = CountDownLatch(1)
    val eventReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action){
                broadcastEvent -> {
                    eventGate.countDown()
                }
            }
        }
    }

    fun waitOnEvent() {
        eventGate.await(timeout, TimeUnit.SECONDS)
    }
}