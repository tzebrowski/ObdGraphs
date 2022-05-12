package org.openobd2.core.logger


import android.content.*
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
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.openobd2.core.logger.ui.preferences.*
import java.lang.ref.WeakReference


const val ACTIVITY_LOGGER_TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    internal var activityBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            receive(context,intent)
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

    private fun setupProgressBar() {
        (findViewById<ProgressBar>(R.id.p_bar)).run {
            visibility = View.GONE
        }
    }

    private fun setupNavigationBar() {
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = navController()

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_gauge,
                R.id.navigation_graph,
                R.id.navigation_dashboard,
                R.id.navigation_debug,
                R.id.navigation_metrics,
                R.id.navigation_preferences
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val mainActivityPreferences = getMainActivityPreferences()
        findViewById<BottomNavigationView>(R.id.nav_view).menu.run {
            findItem(R.id.navigation_debug)?.isVisible =
                mainActivityPreferences.showDebugView

            findItem(R.id.navigation_dashboard).isVisible =
                mainActivityPreferences.showDashView

            findItem(R.id.navigation_gauge).isVisible =
                mainActivityPreferences.showGaugeView

            findItem(R.id.navigation_metrics).isVisible =
                mainActivityPreferences.showMetricsView

            findItem(R.id.navigation_graph).isVisible =
                mainActivityPreferences.showGraphView
        }

        navView.selectedItemId = R.id.navigation_gauge
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


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
    }

    private fun unregisterReceiver() {
        unregisterReceiver(activityBroadcastReceiver)
        unregisterReceiver(tripRecorderBroadcastReceiver)
        unregisterReceiver(powerReceiver)
    }




}
