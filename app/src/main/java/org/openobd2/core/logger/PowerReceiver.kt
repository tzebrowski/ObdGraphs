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
private const val POWER_ON_PREFERENCE_KEY = "pref.adapter.connect.on.power"

class PowerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (Preferences.isEnabled(context!!, POWER_ON_PREFERENCE_KEY)) {
            DataLogger.INSTANCE.init(context)

            if (intent.action === Intent.ACTION_POWER_CONNECTED) {

                Log.i(LOGGER_TAG, "Received ACTION_POWER_CONNECTED action. Start data logging")
                if (!isActivityVisibleOnTheScreen(context, MainActivity::class.java)) {
                    val i = Intent(context, MainActivity::class.java)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(i)
                }
                DataLoggerService.startAction(context)
            } else if (intent.action === Intent.ACTION_POWER_DISCONNECTED) {
                Log.i(
                    LOGGER_TAG,
                    "Received ACTION_POWER_DISCONNECTED action. Stop data logging"
                )
                DataLoggerService.stopAction(context)
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