package org.obd.graphs.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import org.obd.graphs.*
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.profile.setupProfiles
import org.obd.graphs.preferences.updateString

const val ACTIVITY_LOGGER_TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    internal var activityBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            receive(intent)
        }
    }

    private val cache: MutableMap<String, Any> = mutableMapOf()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (getMainActivityPreferences().hideToolbarLandscape) {
            val layout: CoordinatorLayout = this.findViewById(R.id.coordinator_Layout)
            layout.isVisible = newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE
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
        setupProgressBar()
        setupWindowManager()
        setupNavigationBar()
        setupNavigationBarButtons()
        registerReceiver()

        setupExceptionHandler()
        setupProfiles()
        setupStatusPanel()
        setupPreferences()
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

    private fun setupProgressBar() {
        (findViewById<ProgressBar>(R.id.p_bar)).run {
            visibility = View.GONE
        }
    }

    private fun setupExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler())
    }

    private fun setupPreferences() {
        Prefs.updateString("pref.about.build_version", BuildConfig.VERSION_NAME)
    }
}
