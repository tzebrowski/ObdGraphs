package org.openobd2.core.logger

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.util.Log
import org.openobd2.core.logger.bl.DataLogger
import org.openobd2.core.logger.bl.DataLoggerService
import org.openobd2.core.logger.ui.preferences.Preferences


private const val LOGGER_TAG = "POW_RECEIVER"
private const val ADAPTER_CONNECT_PREFERENCE_KEY = "pref.adapter.power.connect_adapter"
private const val SCREEN_ON_OFF_PREFERENCE_KEY = "pref.adapter.power.screen_off"
private const val BT_ON_OFF_PREFERENCE_KEY = " pref.adapter.power.bt_off"

const val SCREEN_OFF = "power.screen.off"
const val SCREEN_ON = "power.screen.on"

class PowerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        Log.i(LOGGER_TAG, "Received Power Event: ${intent.action}")

        if (intent.action === Intent.ACTION_POWER_CONNECTED) {

            if (Preferences.isEnabled(context!!, BT_ON_OFF_PREFERENCE_KEY)) {
                BluetoothAdapter.getDefaultAdapter().run {
                    enable()
                    startDiscovery()
                }
            }

            if (Preferences.isEnabled(context!!, ADAPTER_CONNECT_PREFERENCE_KEY)) {
                DataLoggerService.startAction(context)
            }

            if (Preferences.isEnabled(context, SCREEN_ON_OFF_PREFERENCE_KEY)) {
                DataLogger.INSTANCE.init(context)
                Log.i(LOGGER_TAG, "Start data logging")
                startMainActivity(context)
                context.sendBroadcast(Intent().apply {
                    action = SCREEN_ON
                })
            }
        } else if (intent.action === Intent.ACTION_POWER_DISCONNECTED) {

            if (Preferences.isEnabled(context!!, BT_ON_OFF_PREFERENCE_KEY)) {
                BluetoothAdapter.getDefaultAdapter().run {
                    disable()
                }
            }

            if (Preferences.isEnabled(context!!, ADAPTER_CONNECT_PREFERENCE_KEY)) {
                Log.i(
                    LOGGER_TAG,
                    "Stop data logging"
                )
                DataLoggerService.stopAction(context)
            }

            if (Preferences.isEnabled(context, SCREEN_ON_OFF_PREFERENCE_KEY)) {
                context.sendBroadcast(Intent().apply {
                    action = SCREEN_OFF
                })
            }
        }
    }

    private fun startMainActivity(context: Context) {
        if (!isActivityVisibleOnTheScreen(context, MainActivity::class.java)) {
            val i = Intent(context, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    private fun isActivityVisibleOnTheScreen(context: Context, activityClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val taskInfo = activityManager.getRunningTasks(1)
        Log.d(LOGGER_TAG, "Current top activity ${taskInfo[0].topActivity!!.className}")
        val componentInfo = taskInfo[0].topActivity
        return activityClass.canonicalName.equals(componentInfo!!.className, ignoreCase = true)
    }
}