package org.openobd2.core.logger


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
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import org.openobd2.core.logger.ui.preferences.Prefs
import java.lang.ref.WeakReference

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
        ApplicationContext = WeakReference(this)
        Cache = cache

        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .permitAll().build()
        )

        setContentView(R.layout.activity_main)
        setupPreferences()
        setupProgressBar()
        setupWindowManager()
        setupNavigationBar()
        setupNavigationBarButtons()
        registerReceiver()
        registerExceptionHandler()

        installProfiles()
    }

    private fun registerExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler())
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

    fun navController(): NavController {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController
    }

    private fun setupPreferences() {
        Prefs = PreferenceManager.getDefaultSharedPreferences(this)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
    }

}
