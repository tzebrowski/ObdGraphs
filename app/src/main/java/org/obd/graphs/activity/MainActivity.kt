package org.obd.graphs.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import org.obd.graphs.*
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.profile.vehicleProfile
import org.obd.graphs.preferences.updateBoolean
import org.obd.graphs.preferences.updateString
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

const val ACTIVITY_LOGGER_TAG = "MainActivity"

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityContext(this)
        Cache = cache

        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .permitAll().build()
        )

        setContentView(R.layout.activity_main)

        setupWindowManager()
        setupNavigationBar()
        setupNavigationBarButtons()
        registerReceiver()

        setupExceptionHandler()
        vehicleProfile.setupProfiles()
        setupStatusPanel()
        setupPreferences()

        progressBar {
            it.visibility = View.GONE
        }
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
    }

    private fun setupExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler())
    }

    private fun setupPreferences() {
        Prefs.updateString("pref.about.build_version", BuildConfig.VERSION_NAME)
        Prefs.updateBoolean("pref.debug.logging.enabled", false)
    }
}
