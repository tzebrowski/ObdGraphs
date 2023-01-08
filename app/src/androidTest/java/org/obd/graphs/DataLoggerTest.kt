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
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTED_EVENT
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

    @Test
    fun wifiConnectionOkTest() {
        val countDownLatchBroadcastReceiver = CountDownLatchBroadcastReceiver(
            DATA_LOGGER_CONNECTED_EVENT, 5L)

        launchActivity<MainActivity>().use { it ->
            it.onActivity { activity ->
                activity.registerReceiver(countDownLatchBroadcastReceiver.eventReceiver,
                    IntentFilter(countDownLatchBroadcastReceiver.broadcastEvent))
            }

            // given
            vehicleProfile.loadProfile("profile_5")
            Prefs.updateString("pref.adapter.connection.type","wifi")
            Prefs.updateString("pref.adapter.connection.tcp.host","192.0.0.2")
            Prefs.updateString("pref.adapter.connection.tcp.port","35001")

            val requestResponse = mutableMapOf(
                "ATD" to "ATD OK",
                "ATZ" to "ATZ OK",
                "ATL0" to "ATL0 OK",
                "ATH0" to "ATH0 OK",
                "ATE0" to "ATE0 OK",
                "ATPP 2CSV 01" to "ATPP 2CSV 01 OK",
                "ATPP 2C ON" to "ATPP 2C ON OK",
                "ATPP 2DSV 01" to "ATPP 2DSV 01 OK",
                "ATPP 2D ON" to "ATPP 2D ON OK",
                "ATAT2" to "ATAT2 OK",
                "ATSP0" to "ATSP0 OK",
                "0902" to "SEARCHING...0140:4902015756571:5A5A5A314B5A412:4D363930333932",
                "0100"  to "4100be3ea813",
                "0200" to "4140fed00400",
                "01 15 0B 04 11 0F 05" to "nodata"
            )

            val mockServer = MockServer(port = 35001, requestResponse = requestResponse)

            try {

                runAsync {
                    mockServer.launch()
                    dataLogger.start()
                }
            } finally {

                countDownLatchBroadcastReceiver.waitOnEvent()

                it.onActivity { activity ->
                    activity.unregisterReceiver(countDownLatchBroadcastReceiver.eventReceiver)
                }

                mockServer.stop()
                dataLogger.stop()
            }
        }

        assertEquals("Did not receive broadcast event: ${countDownLatchBroadcastReceiver.broadcastEvent}",
            countDownLatchBroadcastReceiver.eventGate.count,0)
    }
}


