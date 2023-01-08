package org.obd.graphs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.test.core.app.launchActivity
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.activity.MainActivity
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ADAPTER_NOT_SET_EVENT
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.preferences.profile.vehicleProfile
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4ClassRunner::class)
class DataLoggerTest {

    @Test
    fun adapterNotSetTest() {
        val broadcastEvent = DATA_LOGGER_ADAPTER_NOT_SET_EVENT
        val broadcastEventGate  = CountDownLatch(1)

        val broadcastEventReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action){
                    broadcastEvent -> {
                        broadcastEventGate.countDown()
                    }
                }
            }
        }

        launchActivity<MainActivity>().use { it ->
            it.onActivity { activity ->
                activity.registerReceiver(broadcastEventReceiver, IntentFilter(broadcastEvent))
            }
            // lets use this profiles as default
            vehicleProfile.loadProfile("profile_5")
            try {
                runAsync {
                    dataLogger.start()
                }
            } finally {

                broadcastEventGate.await(5,TimeUnit.SECONDS)

                it.onActivity { activity ->
                    activity.unregisterReceiver(broadcastEventReceiver)
                }
            }
        }

        assertEquals("Did not receive broadcast event: $broadcastEvent", broadcastEventGate.count,0)
    }
}
