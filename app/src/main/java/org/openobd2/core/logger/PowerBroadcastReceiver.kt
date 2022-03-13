package org.openobd2.core.logger

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.util.Log
import org.openobd2.core.logger.bl.datalogger.DataLoggerService


private const val LOGGER_TAG = "PowerBroadcastReceiver"

const val SCREEN_OFF_EVENT = "power.screen.off"
const val SCREEN_ON_EVENT = "power.screen.on"

class PowerBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent) {
        Log.i(LOGGER_TAG, "Received Power Event: ${intent.action}")
        val powerPreferences: PowerPreferences = getPowerPreferences()

        if (intent.action === Intent.ACTION_POWER_CONNECTED) {

            if (powerPreferences.btOnOff) {
                BluetoothAdapter.getDefaultAdapter().run {
                    enable()
                    startDiscovery()
                }
            }

            if (powerPreferences.connectOnPower) {
                DataLoggerService.startAction(context!!)
            }

            if (powerPreferences.screenOnOff) {
                Log.i(LOGGER_TAG, "Start data logging")
                startMainActivity(context!!)
                context!!.sendBroadcast(Intent().apply {
                    action = SCREEN_ON_EVENT
                })
            }
        } else if (intent.action === Intent.ACTION_POWER_DISCONNECTED) {

            if (powerPreferences.btOnOff) {
                BluetoothAdapter.getDefaultAdapter().run {
                    disable()
                }
            }

            if (powerPreferences.connectOnPower) {
                Log.i(
                    LOGGER_TAG,
                    "Stop data logging"
                )
                DataLoggerService.stopAction(context!!)
            }

            if (powerPreferences.screenOnOff) {
                context!!.sendBroadcast(Intent().apply {
                    action = SCREEN_OFF_EVENT
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