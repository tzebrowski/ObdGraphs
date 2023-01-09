package org.obd.graphs

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTED_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_DTC_AVAILABLE
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ERROR_EVENT
import org.obd.graphs.preferences.*

@RunWith(AndroidJUnit4ClassRunner::class)
class DataLoggerWifiTest {

    @Test
    fun connectionErrorTest() {
        tcpTestRunner(expectedEventType = DATA_LOGGER_ERROR_EVENT, mockServerPort = 35000, profile = "profile_5"){
            // arrange
            Prefs.updateString("pref.adapter.connection.type","wifi")
            Prefs.updateString("pref.adapter.connection.tcp.host","192.168.0.11")
        }
    }

    @Test
    fun connectionEstablishedTest() {

        tcpTestRunner(expectedEventType = DATA_LOGGER_CONNECTED_EVENT,
            mockServerRequestResponse = mapOf("01 15 0B 04 11 0F 05" to "nodata"),
            mockServerPort = 35007, profile = "profile_3"){

            Prefs.updateString("pref.adapter.connection.type","wifi")
            Prefs.updateString("pref.adapter.connection.tcp.host","192.0.0.2")
            assertTrue(vehicleCapabilitiesManager.getVehicleCapabilities().isEmpty())
        }
    }

    @Test
    fun fetchDTCTest() {

        tcpTestRunner(expectedEventType = DATA_LOGGER_DTC_AVAILABLE,
            mockServerRequestResponse = mapOf( "19020D" to "00F0:5902CF26E4001:482BC10048D0082:00480"),
            mockServerPort = 35002, profile = "profile_3"){

            Prefs.updateString("pref.adapter.connection.type", "wifi")
            Prefs.updateString("pref.adapter.connection.tcp.host", "192.0.0.2")
            Prefs.updateBoolean("pref.adapter.init.fetchDTC", true)
            Prefs.updateStringSet("pref.datalogger.dtc", emptyList())
            assertTrue(vehicleCapabilitiesManager.getVehicleCapabilities().isEmpty())
        }

        assertTrue(vehicleCapabilitiesManager.getDTC().contains("26E400"))
        assertTrue(vehicleCapabilitiesManager.getDTC().contains("D00800"))
        assertTrue(vehicleCapabilitiesManager.getDTC().contains("2BC100"))
    }

    @Test
    fun metadataReadTest() {
        tcpTestRunner(expectedEventType = DATA_LOGGER_CONNECTED_EVENT,
            mockServerRequestResponse = mapOf("22F191" to "00E0:62F1913532301:353533323020202:20",
                                    "22F192" to "00E0:62F1924D4D311:304A41485732332:32",
                                    "22F187" to "00E0:62F1873530351:353938353220202:20",
                                    "22F190" to "0140:62F1905A41521:454145424E394B2:37363137323839",
                                    "22F18C" to "0120:62F18C5444341:313930393539452:3031343430",
                                    "22F194" to "00E0:62F1945031341:315641304520202:20",
                                    "221008" to "6210080000BFC8",
                                    "222008" to "6220080000BFC7",
                                    "22F195" to "62F1950000",
                                    "22F193" to "62F19300"),
            mockServerPort = 35003, profile = "profile_3"){

            Prefs.updateString("pref.adapter.connection.type", "wifi")
            Prefs.updateString("pref.adapter.connection.tcp.host", "192.0.0.2")
            Prefs.getStringSet("pref.datalogger.supported.pids", emptySet())
            assertTrue(vehicleCapabilitiesManager.getVehicleCapabilities().isEmpty())
        }

        assertTrue(vehicleCapabilitiesManager.getVehicleCapabilities().isNotEmpty())
    }
}