 /**
 * Copyright 2019-2026, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs

import android.util.Log
import androidx.test.core.app.launchActivity
import org.junit.Assert
import org.obd.graphs.activity.MainActivity
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.bl.query.Query
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateString
import org.obd.graphs.profile.profile

private const val CONNECTION_TYPE = "wifi"
private const val MOCK_SERVER_PORT = "192.0.0.2"

private const val LOG_TAG = "tcpTestRunner"

fun tcpTestRunner(
    givenProfile: String,
    expectedEventType: String,
    mockServerRequestResponse: Map<String, String> = emptyMap(),
    mockServerPort: Int,
    act: () -> Unit = {},
    assert: () -> Unit = {},
    arrange: () -> Unit,
) {
    val receiver = CountDownLatchBroadcastReceiver(expectedEventType)
    launchActivity<MainActivity>().use { it ->

        it.onActivity { activity ->
            registerReceiver(activity, receiver.eventReceiver) {
                it.addAction(receiver.broadcastEvent)
            }
        }

        profile.reset()
        profile.loadProfile(givenProfile)

        Prefs.updateString("pref.adapter.connection.type", CONNECTION_TYPE)
        Prefs.updateString("pref.adapter.connection.tcp.host", MOCK_SERVER_PORT)
        Prefs.updateString("pref.adapter.connection.tcp.port", "$mockServerPort")
        dataLoggerSettings.reload()

        val mockServer = MockServer(port = mockServerPort, requestResponse = mockServerRequestResponse)

        try {
            arrange.invoke()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to execute 'arrange' test section", e)
        }

        dataLoggerSettings.reload()

        try {
            runAsync {
                mockServer.launch()
                dataLogger.start(Query.instance())
            }

            try {
                act.invoke()
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to execute  'act' test section", e)
            }
        } finally {
            receiver.waitOnEvent()

            it.onActivity { activity ->
                activity.unregisterReceiver(receiver.eventReceiver)
            }
            dataLogger.stop()
            mockServer.stop()
        }
    }

    Assert.assertEquals(
        "Did not receive broadcast event: ${receiver.broadcastEvent}",
        receiver.eventGate.count,
        0,
    )

    try {
        assert.invoke()
    } catch (e: Exception) {
        Log.e(LOG_TAG, "Failed to execute 'assert' test  section", e)
    }
}
