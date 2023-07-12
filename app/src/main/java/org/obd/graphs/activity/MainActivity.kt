package org.obd.graphs.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import org.obd.graphs.*
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.profile.vehicleProfile
import org.obd.graphs.preferences.updateBoolean
import org.obd.graphs.preferences.updateString
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.util.*


const val LOG_TAG = "MainActivity"


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    lateinit var lockScreenDialog: AlertDialog

    internal var activityBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            receive(intent)
        }
    }
    private val cache: MutableMap<String, Any> = mutableMapOf()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        toolbar {
            if (getMainActivityPreferences().hideToolbarLandscape) {
                it.isVisible = newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE
            }
        }
    }


    override fun onPause() {
        super.onPause()
        sendBroadcastEvent(MAIN_ACTIVITY_EVENT_PAUSE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupStrictMode()
        super.onCreate(savedInstanceState)

        setActivityContext(this)
        initCache()
        setContentView(R.layout.activity_main)

        setupWindowManager()
        setupNavigationBar()
        setupNavigationBarButtons()
        registerReceiver()

        setupExceptionHandler()
        vehicleProfile.setupProfiles()
        setupStatusPanel()
        setupPreferences()

        network.setupConnectedNetworksCallback()

        progressBar {
            it.visibility = View.GONE
        }

        setupLockScreenDialog()
    }

    override fun onResume() {
        super.onResume()
        setupWindowManager()
        changeScreenBrightness(1f)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
        sendBroadcastEvent(MAIN_ACTIVITY_EVENT_DESTROYED)
    }

    private fun setupExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler())
    }

    private fun setupPreferences() {
        Prefs.updateString("pref.about.build_time", "${SimpleDateFormat("yyyyMMdd.HHmm", Locale.getDefault()).parse(BuildConfig.VERSION_NAME)}")
        Prefs.updateString("pref.about.build_version", "${BuildConfig.VERSION_CODE}")
        Prefs.updateBoolean("pref.debug.logging.enabled", false)
    }

    private fun initCache() {
        cacheManager.initCache(cache)
    }

    private fun setupLockScreenDialog() {
        AlertDialog.Builder(this).run {
            setCancelable(false)

            val dialogView: View = this@MainActivity.layoutInflater.inflate(R.layout.dialog_screen_lock, null)
            setView(dialogView)
            lockScreenDialog = create()
        }
    }

    private fun setupStrictMode() {
        if (BuildConfig.DEBUG && false) {
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build()
            )

            StrictMode.setVmPolicy(VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
        }
    }
}
