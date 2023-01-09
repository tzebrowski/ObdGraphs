package org.obd.graphs

import android.content.IntentFilter
import androidx.test.core.app.launchActivity
import org.junit.Assert
import org.obd.graphs.activity.MainActivity
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.profile.vehicleProfile
import org.obd.graphs.preferences.updateString

private const val CONNECTION_TYPE = "wifi"
private const val TCP_HOST = "192.0.0.2"

fun tcpTestRunner (profile: String,
                   expectedEventType: String,
                   mockServerRequestResponse: Map<String,String> = emptyMap(),
                   mockServerPort: Int,
                   arrange: () -> Unit){

    val receiver = CountDownLatchBroadcastReceiver(expectedEventType)
    launchActivity<MainActivity>().use {

        it.onActivity { activity ->
            activity.registerReceiver(receiver.eventReceiver,
                IntentFilter(receiver.broadcastEvent)
            )
        }

        vehicleProfile.reset()
        vehicleProfile.loadProfile(profile)

        Prefs.updateString("pref.adapter.connection.type", CONNECTION_TYPE)
        Prefs.updateString("pref.adapter.connection.tcp.host", TCP_HOST)
        Prefs.updateString("pref.adapter.connection.tcp.port", "$mockServerPort")
        dataLoggerPreferences.reload()

        val mockServer = MockServer(port = mockServerPort, requestResponse = mockServerRequestResponse)

        arrange.invoke()

        try {
            runAsync {
                mockServer.launch()
                dataLogger.start()
            }
        } finally {

            receiver.waitOnEvent()

            it.onActivity { activity ->
                activity.unregisterReceiver(receiver.eventReceiver)
            }

            mockServer.stop()
            dataLogger.stop()
        }
    }

    Assert.assertEquals(
        "Did not receive broadcast event: ${receiver.broadcastEvent}",
        receiver.eventGate.count, 0
    )
}