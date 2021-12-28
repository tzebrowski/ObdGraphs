package org.openobd2.core.logger

import android.app.ActivityManager
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

const val SCREEN_OFF = "power.screen.off"
const val SCREEN_ON = "power.screen.on"

class PowerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action === Intent.ACTION_POWER_CONNECTED) {
            Log.i(LOGGER_TAG, "Received ACTION_POWER_CONNECTED action.")

            if (Preferences.isEnabled(context!!, ADAPTER_CONNECT_PREFERENCE_KEY)) {
                DataLoggerService.startAction(context)
            } else if (Preferences.isEnabled(context, SCREEN_ON_OFF_PREFERENCE_KEY)) {
                DataLogger.INSTANCE.init(context)
                Log.i(LOGGER_TAG, "Start data logging")
                if (!isActivityVisibleOnTheScreen(context, MainActivity::class.java)) {
                    val i = Intent(context, MainActivity::class.java)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(i)
                }

                context.sendBroadcast(Intent().apply {
                    action = SCREEN_ON
                })
            }
        } else if (intent.action === Intent.ACTION_POWER_DISCONNECTED) {
            Log.i(
                LOGGER_TAG,
                "Received ACTION_POWER_DISCONNECTED action."
            )

            if (Preferences.isEnabled(context!!, ADAPTER_CONNECT_PREFERENCE_KEY)) {
                Log.i(
                    LOGGER_TAG,
                    "Stop data logging"
                )

                DataLoggerService.stopAction(context)

            } else if (Preferences.isEnabled(context, SCREEN_ON_OFF_PREFERENCE_KEY)) {
                context.sendBroadcast(Intent().apply {
                    action = SCREEN_OFF
                })
            }
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