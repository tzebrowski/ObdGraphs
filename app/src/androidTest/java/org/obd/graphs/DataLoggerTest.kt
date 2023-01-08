package org.obd.graphs

import android.content.IntentFilter
import androidx.test.core.app.launchActivity
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.activity.MainActivity
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ADAPTER_NOT_SET_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ERROR_EVENT
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.profile.vehicleProfile
import org.obd.graphs.preferences.updateString

@RunWith(AndroidJUnit4ClassRunner::class)
class DataLoggerTest {

    @Test
    fun bluetoothAdapterNotSetTest() {
        val countDownLatchBroadcastReceiver = CountDownLatchBroadcastReceiver(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)

        launchActivity<MainActivity>().use { it ->
            it.onActivity { activity ->
                activity.registerReceiver(countDownLatchBroadcastReceiver.eventReceiver,
                    IntentFilter(countDownLatchBroadcastReceiver.broadcastEvent))
            }
            // lets use this profiles as default
            vehicleProfile.loadProfile("profile_5")
            Prefs.updateString("pref.adapter.id","")
            Prefs.updateString("pref.adapter.connection.type","bluetooth")

            try {
                runAsync {
                    dataLogger.start()
                }
            } finally {

                countDownLatchBroadcastReceiver.waitOnEvent()

                it.onActivity { activity ->
                    activity.unregisterReceiver(countDownLatchBroadcastReceiver.eventReceiver)
                }
            }
        }

        assertEquals("Did not receive broadcast event: ${countDownLatchBroadcastReceiver.broadcastEvent}",
            countDownLatchBroadcastReceiver.eventGate.count,0)
    }

    @Test
    fun wifiConnectionErrorTest() {
        val countDownLatchBroadcastReceiver = CountDownLatchBroadcastReceiver(DATA_LOGGER_ERROR_EVENT)

        launchActivity<MainActivity>().use { it ->
            it.onActivity { activity ->
                activity.registerReceiver(countDownLatchBroadcastReceiver.eventReceiver,
                    IntentFilter(countDownLatchBroadcastReceiver.broadcastEvent))
            }

            // given
            vehicleProfile.loadProfile("profile_5")
            Prefs.updateString("pref.adapter.connection.type","wifi")
            Prefs.updateString("pref.adapter.connection.tcp.host","192.168.0.11")

            try {
                runAsync {
                    dataLogger.start()
                }
            } finally {

                countDownLatchBroadcastReceiver.waitOnEvent()

                it.onActivity { activity ->
                    activity.unregisterReceiver(countDownLatchBroadcastReceiver.eventReceiver)
                }
            }
        }

        assertEquals("Did not receive broadcast event: ${countDownLatchBroadcastReceiver.broadcastEvent}",
            countDownLatchBroadcastReceiver.eventGate.count,0)
    }
}
