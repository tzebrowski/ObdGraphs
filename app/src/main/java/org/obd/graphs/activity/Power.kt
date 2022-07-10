package org.obd.graphs.activity

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.obd.graphs.AdminReceiver

internal fun MainActivity.lockScreen() {
    val pm = getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
    if (pm.isInteractive) {
        val policy =
            getSystemService(AppCompatActivity.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        try {
            policy.lockNow()
        } catch (ex: SecurityException) {
            Toast.makeText(
                this,
                "must enable device administrator",
                Toast.LENGTH_LONG
            ).show()
            val admin = ComponentName(this, AdminReceiver::class.java)
            val intent: Intent = Intent(
                DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN
            ).putExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin
            )
            startActivity(intent)
        }
    }
}

internal fun MainActivity.changeScreenBrightness(value: Float) {
    val pm = getSystemService(AppCompatActivity.POWER_SERVICE) as PowerManager
    val wl = pm.newWakeLock(
        PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
        "data_logger:wakeLock"
    )
    try {

        wl.acquire(5000)//wait 5s
        val params: WindowManager.LayoutParams =
            window.attributes
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        params.screenBrightness = value
        window.attributes = params

    } catch (e: Throwable) {
        Log.e(ACTIVITY_LOGGER_TAG, "Failed to change screen brightness", e)
    } finally {
        wl.release()
    }
}

internal fun MainActivity.hideSystemUI() {
    val decorView = window.decorView
    decorView.systemUiVisibility =
        (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
}

internal fun MainActivity.setupWindowManager() {
    //keeps screen on
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
    window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
}