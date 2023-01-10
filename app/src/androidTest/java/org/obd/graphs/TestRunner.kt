package org.obd.graphs

import android.content.IntentFilter
import android.util.Log
import androidx.test.core.app.launchActivity
import org.junit.Assert
import org.obd.graphs.activity.MainActivity
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.profile.vehicleProfile
import org.obd.graphs.preferences.updateString

private const val CONNECTION_TYPE = "wifi"
private const val MOCK_SERVER_PORT = "192.0.0.2"

private const val LOG_TAG = "tcpTestRunner"

fun tcpTestRunner (profile: String,
                   expectedEventType: String,
                   mockServerRequestResponse: Map<String,String> = emptyMap(),
                   mockServerPort: Int,
                   act: () -> Unit = {},
                   assert: () -> Unit = {},
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
        Prefs.updateString("pref.adapter.connection.tcp.host", MOCK_SERVER_PORT)
        Prefs.updateString("pref.adapter.connection.tcp.port", "$mockServerPort")
        dataLoggerPreferences.reload()

        val mockServer = MockServer(port = mockServerPort, requestResponse = mockServerRequestResponse)

        try {
            arrange.invoke()
        }catch (e: Exception){
            Log.e(LOG_TAG, "Failed to execute 'arrange' test section",e)
        }

        dataLoggerPreferences.reload()

        try {
            runAsync {
                mockServer.launch()
                dataLogger.start()
            }

            try {
                act.invoke()
            }catch (e: Exception){
                Log.e(LOG_TAG, "Failed to execute  'act' test section",e)
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
        receiver.eventGate.count, 0
    )

    try {
        assert.invoke()
    }catch (e: Exception){
        Log.e(LOG_TAG, "Failed to execute 'assert' test  section",e)
    }
}