package org.obd.graphs

import android.content.IntentFilter
import androidx.test.core.app.launchActivity
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.activity.MainActivity
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTED_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_DTC_AVAILABLE
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ERROR_EVENT
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.preferences.*
import org.obd.graphs.preferences.profile.vehicleProfile

@RunWith(AndroidJUnit4ClassRunner::class)
class DataLoggerWifiTest {

    @Test
    fun connectionErrorTest() {
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

                dataLogger.stop()
            }
        }

        assertEquals("Did not receive broadcast event: ${countDownLatchBroadcastReceiver.broadcastEvent}",
            countDownLatchBroadcastReceiver.eventGate.count,0)
    }

    @Test
    fun connectionEstablishedTest() {
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

            val mockServer = MockServer(port = 35001, requestResponse = mutableMapOf(
                "01 15 0B 04 11 0F 05" to "nodata",
            ))

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

    @Test
    fun fetchDTCTest() {
        val countDownLatchBroadcastReceiver = CountDownLatchBroadcastReceiver(
            DATA_LOGGER_DTC_AVAILABLE, 5L)

        launchActivity<MainActivity>().use { it ->
            it.onActivity { activity ->
                activity.registerReceiver(countDownLatchBroadcastReceiver.eventReceiver,
                    IntentFilter(countDownLatchBroadcastReceiver.broadcastEvent))
            }
            val serverPort = 35002
            // given
            vehicleProfile.loadProfile("profile_3")
            Prefs.updateString("pref.adapter.connection.type", "wifi")
            Prefs.updateString("pref.adapter.connection.tcp.host", "192.0.0.2")
            Prefs.updateString("pref.adapter.connection.tcp.port", "$serverPort")
            Prefs.updateBoolean("pref.adapter.init.fetchDTC", true)
            Prefs.updateStringSet("pref.datalogger.dtc", emptyList())

            assertTrue(vehicleCapabilitiesManager.getDTC().isEmpty())

            val mockServer = MockServer(port = serverPort, requestResponse = mutableMapOf(
                "19020D" to "00F0:5902CF26E4001:482BC10048D0082:00480",
            ))

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

        assertTrue(vehicleCapabilitiesManager.getDTC().contains("26E400"))
        assertTrue(vehicleCapabilitiesManager.getDTC().contains("D00800"))
        assertTrue(vehicleCapabilitiesManager.getDTC().contains("2BC100"))
    }

    @Test
    fun metadataReadTest() {
        val countDownLatchBroadcastReceiver = CountDownLatchBroadcastReceiver(
            DATA_LOGGER_CONNECTED_EVENT, 5L)

        launchActivity<MainActivity>().use { it ->
            it.onActivity { activity ->
                activity.registerReceiver(countDownLatchBroadcastReceiver.eventReceiver,
                    IntentFilter(countDownLatchBroadcastReceiver.broadcastEvent))
            }
            val serverPort = 35003
            // given
            vehicleProfile.loadProfile("profile_3")
            Prefs.updateString("pref.adapter.connection.type", "wifi")
            Prefs.updateString("pref.adapter.connection.tcp.host", "192.0.0.2")
            Prefs.updateString("pref.adapter.connection.tcp.port", "$serverPort")

            Prefs.getStringSet("pref.datalogger.supported.pids", emptySet())

            assertTrue(vehicleCapabilitiesManager.getVehicleCapabilities().isEmpty())

            val mockServer = MockServer(port = serverPort, requestResponse = mutableMapOf(
                "22F191" to "00E0:62F1913532301:353533323020202:20",
                "22F192" to "00E0:62F1924D4D311:304A41485732332:32",
                "22F187" to "00E0:62F1873530351:353938353220202:20",
                "22F190" to "0140:62F1905A41521:454145424E394B2:37363137323839",
                "22F18C" to "0120:62F18C5444341:313930393539452:3031343430",
                "22F194" to "00E0:62F1945031341:315641304520202:20",
                "221008" to "6210080000BFC8",
                "222008" to "6220080000BFC7",
                "22F195" to "62F1950000",
                "22F193" to "62F19300",
            ))

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

        //
        assertTrue(vehicleCapabilitiesManager.getVehicleCapabilities().isNotEmpty())
    }
}


