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
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.profile.vehicleProfile
import org.obd.graphs.preferences.updateString

@RunWith(AndroidJUnit4ClassRunner::class)
class DataLoggerBluetoothTest {

    @Test
    fun adapterNotSetTest() {
        val countDownLatchBroadcastReceiver = CountDownLatchBroadcastReceiver(DATA_LOGGER_ADAPTER_NOT_SET_EVENT)

        launchActivity<MainActivity>().use { it ->
            it.onActivity { activity ->
                activity.registerReceiver(countDownLatchBroadcastReceiver.eventReceiver,
                    IntentFilter(countDownLatchBroadcastReceiver.broadcastEvent))
            }
            // lets use this profiles as default
            vehicleProfile.loadProfile("profile_5")
            Prefs.updateString("pref.adapter.id","").commit()
            Prefs.updateString("pref.adapter.connection.type","bluetooth").commit()

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


