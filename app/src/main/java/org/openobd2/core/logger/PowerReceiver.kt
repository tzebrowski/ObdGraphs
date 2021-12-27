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


class PowerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (Preferences.isEnabled(context!!, "pref.adapter.connect.on.power")) {
            DataLogger.INSTANCE.init(context!!)

            if (intent.action === Intent.ACTION_POWER_CONNECTED) {
                Log.i("POW_RECEIVER", "Received ACTION_POWER_CONNECTED action. Start data logging")
                if (!isActivityVisibleOnTheScreen(context, MainActivity::class.java)) {
                    val i = Intent(context, MainActivity::class.java)
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(i)
                }
                DataLoggerService.startAction(context!!)
            } else if (intent.action === Intent.ACTION_POWER_DISCONNECTED) {
                Log.i("POW_RECEIVER", "Received ACTION_POWER_DISCONNECTED action. Stop data logging")
                DataLoggerService.stopAction(context!!)
            }
        }
    }
    private fun isActivityVisibleOnTheScreen(context: Context, activityClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val taskInfo = activityManager.getRunningTasks(1)
        Log.d("POW_RECEIVER", "Current top activity ${taskInfo[0].topActivity!!.className}")
        val componentInfo = taskInfo[0].topActivity
        return activityClass.canonicalName.equals(componentInfo!!.className, ignoreCase = true)
    }
}