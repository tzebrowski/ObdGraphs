package org.openobd2.core.logger


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.openobd2.core.logger.bl.*
import org.openobd2.core.logger.ui.common.TOGGLE_TOOLBAR_ACTION
import org.openobd2.core.logger.ui.preferences.*


class MainActivity : AppCompatActivity() {

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                TOGGLE_TOOLBAR_ACTION -> {
                    if (PreferencesHelper.isEnabled(context!!, "pref.toolbar.hide.doubleclick")) {
                        val layout: CoordinatorLayout = findViewById(R.id.coordinator_Layout)
                        layout.isVisible = !layout.isVisible
                    }
                }

                NOTIFICATION_METRICS_VIEW_SHOW, NOTIFICATION_METRICS_VIEW_HIDE -> {
                    toggleNavigationItem(R.id.navigation_metrics)
                }

                NOTIFICATION_DEBUG_VIEW_HIDE, NOTIFICATION_DEBUG_VIEW_SHOW -> {
                    toggleNavigationItem(R.id.navigation_debug)
                }

                NOTIFICATION_DASH_VIEW_HIDE, NOTIFICATION_DASH_VIEW_SHOW -> {
                    toggleNavigationItem(R.id.navigation_dashboard)
                }

                NOTIFICATION_GAUGE_VIEW_HIDE, NOTIFICATION_GAUGE_VIEW_SHOW -> {
                    toggleNavigationItem(R.id.navigation_gauge)
                }

                NOTIFICATION_CONNECTING -> {
                    toast("Connecting to the device.")

                    val progressBar: ProgressBar = findViewById(R.id.p_bar)
                    progressBar.visibility = View.VISIBLE
                    progressBar.indeterminateDrawable.setColorFilter(
                        Color.parseColor("#C22636"),
                        android.graphics.PorterDuff.Mode.SRC_IN
                    )

                    val btn: FloatingActionButton = findViewById(R.id.action_btn)
                    btn.backgroundTintList = resources.getColorStateList(R.color.purple_200)
                    btn.setOnClickListener(View.OnClickListener {
                        Log.i("DATA_LOGGER_UI", "Stop data logging ")
                        DataLoggerService.stopAction(context!!)
                    })
                    btn.refreshDrawableState()
                }

                NOTIFICATION_CONNECTED -> {
                    toast(
                        "Connection to the device has been established." +
                                "\n Start collecting data from ECU."
                    )

                    val progressBar: ProgressBar = findViewById(R.id.p_bar)
                    progressBar.indeterminateDrawable.setColorFilter(
                        Color.parseColor("#01804F"),
                        android.graphics.PorterDuff.Mode.SRC_IN
                    )
                }

                NOTIFICATION_STOPPED -> {
                    toast("Connection with the device has been stopped.")
                    handleStop(context!!)
                }

                NOTIFICATION_ERROR -> {
                    toast("Error occurred during. Please check your connection.")
                    handleStop(context!!)
                }
            }
        }

        private fun handleStop(context: Context) {
            val progressBar: ProgressBar = findViewById(R.id.p_bar)
            progressBar.visibility = View.GONE

            val btn: FloatingActionButton = findViewById(R.id.action_btn)
            btn.backgroundTintList = resources.getColorStateList(R.color.purple_500)
            btn.setOnClickListener(View.OnClickListener {
                Log.i("DATA_LOGGER_UI", "Stop data logging ")
                DataLoggerService.startAction(context)
            })
        }

        private fun toggleNavigationItem(id: Int) {
            val menuItem =
                findViewById<BottomNavigationView>(R.id.nav_view).menu.findItem(id)
            menuItem.isVisible = !menuItem.isVisible
        }

        private fun toast(text: String) {
            val toast = Toast.makeText(
                applicationContext, text,
                Toast.LENGTH_LONG
            )
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (PreferencesHelper.isEnabled(this.applicationContext, "pref.toolbar.hide.landscape")) {
            val layout: CoordinatorLayout = this.findViewById(R.id.coordinator_Layout)
            layout.isVisible = newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val policy = ThreadPolicy.Builder()
            .permitAll().build()
        StrictMode.setThreadPolicy(policy)

        DataLogger.INSTANCE.init(this.application)

        setContentView(R.layout.activity_main)
        setupNavigation()

        loadPreferences()
        registerReceiver()

        val progressBar: ProgressBar = findViewById(R.id.p_bar)
        progressBar.visibility = View.GONE

        val btnStart: FloatingActionButton = findViewById(R.id.action_btn)
        btnStart.setOnClickListener(View.OnClickListener {
            Log.i("DATA_LOGGER_UI", "Start data logging")
            DataLoggerService.startAction(this)
        })
    }


    override fun onResume() {
        super.onResume()
        registerReceiver()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        val decorView = window.decorView
        decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private fun setupNavigation() {
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard,
                R.id.navigation_gauge,
                R.id.navigation_debug,
                R.id.navigation_metrics,
                R.id.navigation_configuration
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private fun loadPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        findViewById<BottomNavigationView>(R.id.nav_view).menu.findItem(R.id.navigation_debug).isVisible =
            PreferencesHelper.isEnabled(this, "pref.debug.view.enabled")

        findViewById<BottomNavigationView>(R.id.nav_view).menu.findItem(R.id.navigation_dashboard).isVisible =
            PreferencesHelper.isEnabled(this, "pref.dash.view.enabled")

        findViewById<BottomNavigationView>(R.id.nav_view).menu.findItem(R.id.navigation_gauge).isVisible =
            PreferencesHelper.isEnabled(this, "pref.gauge.view.enabled")

        findViewById<BottomNavigationView>(R.id.nav_view).menu.findItem(R.id.navigation_metrics).isVisible =
            PreferencesHelper.isEnabled(this, "pref.metrics.view.enabled")
    }


    private fun registerReceiver() {
        registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(NOTIFICATION_CONNECTING)
            addAction(NOTIFICATION_STOPPED)
            addAction(NOTIFICATION_STOPPING)
            addAction(NOTIFICATION_ERROR)
            addAction(NOTIFICATION_CONNECTED)

            addAction(NOTIFICATION_DEBUG_VIEW_SHOW)
            addAction(NOTIFICATION_DEBUG_VIEW_HIDE)
            addAction(NOTIFICATION_GAUGE_VIEW_SHOW)
            addAction(NOTIFICATION_GAUGE_VIEW_HIDE)
            addAction(NOTIFICATION_DASH_VIEW_SHOW)
            addAction(NOTIFICATION_DASH_VIEW_HIDE)
            addAction(TOGGLE_TOOLBAR_ACTION)
            addAction(NOTIFICATION_METRICS_VIEW_SHOW)
            addAction(NOTIFICATION_METRICS_VIEW_HIDE)
        })
    }
}
