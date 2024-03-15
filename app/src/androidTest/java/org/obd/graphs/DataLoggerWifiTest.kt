/**
 * Copyright 2019-2024, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.obd.graphs

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.datalogger.DATA_LOGGER_CONNECTED_EVENT
import org.obd.graphs.bl.datalogger.DATA_LOGGER_DTC_AVAILABLE
import org.obd.graphs.bl.datalogger.DATA_LOGGER_ERROR_EVENT
import org.obd.graphs.bl.datalogger.vehicleCapabilitiesManager
import org.obd.graphs.preferences.*

@RunWith(AndroidJUnit4ClassRunner::class)
class DataLoggerWifiTest {

    @Test
    fun connectionErrorTest() {
        tcpTestRunner(
            arrange = {
                // arrange
                Prefs.updateString("pref.adapter.connection.type","wifi")
                Prefs.updateString("pref.adapter.connection.tcp.host","192.168.0.11")
            },
            expectedEventType = DATA_LOGGER_ERROR_EVENT,
            mockServerPort = 35000,
            givenProfile = "profile_5"
        )
    }

    @Test
    fun connectionEstablishedTest() {

        tcpTestRunner(
            arrange = {
                // arrange
                Prefs.updateString("pref.adapter.connection.type","wifi")
                Prefs.updateString("pref.adapter.connection.tcp.host","192.0.0.2")
                assertTrue(vehicleCapabilitiesManager.getVehicleCapabilities().isEmpty())
            },
            expectedEventType = DATA_LOGGER_CONNECTED_EVENT,
            mockServerRequestResponse = mapOf("01 15 0B 04 11 0F 05" to "nodata"),
            mockServerPort = 35007,
            givenProfile = "profile_3"
        )
    }

    @Test
    fun fetchDTCTest() {

        tcpTestRunner(
            assert = {
                assertTrue(vehicleCapabilitiesManager.getDTC().contains("26E400"))
                assertTrue(vehicleCapabilitiesManager.getDTC().contains("D00800"))
                assertTrue(vehicleCapabilitiesManager.getDTC().contains("2BC100"))
            },
            arrange = {
                // arrange
                Prefs.updateString("pref.adapter.connection.type", "wifi")
                Prefs.updateString("pref.adapter.connection.tcp.host", "192.0.0.2")
                Prefs.updateBoolean("pref.adapter.init.fetchDTC", true)
                Prefs.updateStringSet("pref.datalogger.dtc", emptyList())

                assertTrue(vehicleCapabilitiesManager.getVehicleCapabilities().isEmpty())
            },
            expectedEventType = DATA_LOGGER_DTC_AVAILABLE,
            mockServerRequestResponse = mapOf( "19020D" to "00F0:5902CF26E4001:482BC10048D0082:00480"),
            mockServerPort = 35009,
            givenProfile = "profile_3"
        )
    }

    @Test
    fun metadataReadTest() {
        tcpTestRunner(
            arrange = {
                Prefs.updateString("pref.adapter.connection.type", "wifi")
                Prefs.updateString("pref.adapter.connection.tcp.host", "192.0.0.2")
                Prefs.getStringSet("pref.datalogger.supported.pids", emptySet())
                assertTrue(vehicleCapabilitiesManager.getVehicleCapabilities().isEmpty())
            },
            assert = {
                assertTrue(vehicleCapabilitiesManager.getVehicleCapabilities().isNotEmpty())
            },
            expectedEventType = DATA_LOGGER_CONNECTED_EVENT,
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
            mockServerPort = 35003,
            givenProfile = "profile_3"
        )
    }


    @Test
    fun supportedPIDsTest() {
        tcpTestRunner(
            arrange = {
                // arrange
                Prefs.updateString("pref.adapter.connection.type", "wifi")
                Prefs.updateString("pref.adapter.connection.tcp.host", "192.0.0.2")
                Prefs.updateStringSet("pref.datalogger.supported.pids", emptyList())

                assertTrue(vehicleCapabilitiesManager.getCapabilities().isEmpty())
            },
            assert = {
                assertTrue(vehicleCapabilitiesManager.getCapabilities().isNotEmpty())
                assertTrue(vehicleCapabilitiesManager.getCapabilities().contains("01"))
                assertTrue(vehicleCapabilitiesManager.getCapabilities().contains("03"))
                assertTrue(vehicleCapabilitiesManager.getCapabilities().contains("04"))
                assertTrue(vehicleCapabilitiesManager.getCapabilities().contains("05"))
                assertTrue(vehicleCapabilitiesManager.getCapabilities().contains("06"))
                assertTrue(vehicleCapabilitiesManager.getCapabilities().contains("07"))
            },
            expectedEventType = DATA_LOGGER_CONNECTED_EVENT,
            mockServerRequestResponse = mapOf("0100" to "4100BE3DA813410098180001",
                "0200" to "4120801FB011412080018001",
                "0400" to "4140FED09081414040800000",
                "0600" to "416001214000"),
            mockServerPort = 35008,
            givenProfile = "profile_3"
        )

    }
}