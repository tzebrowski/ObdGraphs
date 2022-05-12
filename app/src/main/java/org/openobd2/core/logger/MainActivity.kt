package org.openobd2.core.logger


import android.app.admin.DevicePolicyManager
import android.content.*
import android.content.Intent.ACTION_BATTERY_CHANGED
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.os.PowerManager
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.openobd2.core.logger.bl.datalogger.*
import org.openobd2.core.logger.bl.trip.TripRecorderBroadcastReceiver
import org.openobd2.core.logger.ui.common.TOGGLE_TOOLBAR_ACTION
import org.openobd2.core.logger.ui.common.toast
import org.openobd2.core.logger.ui.preferences.*
import java.lang.ref.WeakReference


private const val LOGGER_TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private var activityBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                TOGGLE_TOOLBAR_ACTION -> {
                    if (getMainActivityPreferences().hideToolbarDoubleClick) {
                        val layout: CoordinatorLayout = findViewById(R.id.coordinator_Layout)
                        layout.isVisible = !layout.isVisible
                    }
                }
                SCREEN_OFF_EVENT -> {
                    lockScreen()
                }
                SCREEN_ON_EVENT -> {
                    Log.i(LOGGER_TAG, "Activating application.")
                    changeScreenBrightness(1f)
                }
                DATA_LOGGER_ERROR_CONNECT_EVENT -> {
                    toast(R.string.main_activity_toast_connection_connect_error)
                }
                NOTIFICATION_METRICS_VIEW_TOGGLE -> {
                    toggleNavigationItem(R.id.navigation_metrics)
                }

                NOTIFICATION_GRAPH_VIEW_TOGGLE -> {
                    toggleNavigationItem(R.id.navigation_graph)
                }

                NOTIFICATION_DEBUG_VIEW_TOGGLE -> {
                    toggleNavigationItem(R.id.navigation_debug)
                }

                NOTIFICATION_DASH_VIEW_TOGGLE -> {
                    toggleNavigationItem(R.id.navigation_dashboard)
                }

                NOTIFICATION_GAUGE_VIEW_TOGGLE -> {
                    toggleNavigationItem(R.id.navigation_gauge)
                }

                DATA_LOGGER_CONNECTING_EVENT -> {
                    toast(R.string.main_activity_toast_connection_connecting)
                    val progressBar: ProgressBar = findViewById(R.id.p_bar)
                    progressBar.visibility = View.VISIBLE
                    progressBar.indeterminateDrawable.colorFilter = PorterDuffColorFilter(
                        Color.parseColor("#C22636"),
                        PorterDuff.Mode.SRC_IN
                    )

                    val btn: FloatingActionButton = findViewById(R.id.connect_btn)
                    btn.backgroundTintList =
                        ContextCompat.getColorStateList(applicationContext, R.color.purple_200)
                    btn.setOnClickListener {
                        Log.i(LOGGER_TAG, "Stop data logging ")
                        DataLoggerService.stopAction(context!!)
                    }
                    btn.refreshDrawableState()
                }

                DATA_LOGGER_CONNECTED_EVENT -> {
                    toast(R.string.main_activity_toast_connection_established)

                    val progressBar: ProgressBar = findViewById(R.id.p_bar)
                    progressBar.indeterminateDrawable.colorFilter = PorterDuffColorFilter(
                        Color.parseColor("#01804F"),
                        PorterDuff.Mode.SRC_IN
                    )

                    if (getMainActivityPreferences().hideToolbarConnected) {
                        val layout: CoordinatorLayout = findViewById(R.id.coordinator_Layout)
                        layout.isVisible = false
                    }
                }

                DATA_LOGGER_STOPPED_EVENT -> {
                    toast(R.string.main_activity_toast_connection_stopped)
                    handleStop(context!!)
                }

                DATA_LOGGER_ERROR_EVENT -> {
                    toast(R.string.main_activity_toast_connection_error)
                    handleStop(context!!)
                }
            }
        }

        private fun handleStop(context: Context) {
            val progressBar: ProgressBar = findViewById(R.id.p_bar)
            progressBar.visibility = View.GONE

            val btn: FloatingActionButton = findViewById(R.id.connect_btn)
            btn.backgroundTintList =
                ContextCompat.getColorStateList(applicationContext, R.color.purple_500)
            btn.setOnClickListener {
                Log.i(LOGGER_TAG, "Stop data logging ")
                DataLoggerService.startAction(context)
            }

            if (getMainActivityPreferences().hideToolbarConnected) {
                val layout: CoordinatorLayout = findViewById(R.id.coordinator_Layout)
                layout.isVisible = true
            }
        }

        private fun toggleNavigationItem(id: Int) {
            findViewById<BottomNavigationView>(R.id.nav_view).menu.findItem(id)?.run {
                this.isVisible = !this.isVisible
            }
        }
    }

    private val cache: MutableMap<String, Any> = mutableMapOf()
    private val tripRecorderBroadcastReceiver = TripRecorderBroadcastReceiver()
    private val powerReceiver = PowerBroadcastReceiver()

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

    private fun registerReceiver() {

        registerReceiver(activityBroadcastReceiver, IntentFilter().apply {
            addAction(DATA_LOGGER_CONNECTING_EVENT)
            addAction(DATA_LOGGER_STOPPED_EVENT)
            addAction(DATA_LOGGER_STOPPING_EVENT)
            addAction(DATA_LOGGER_ERROR_EVENT)
            addAction(DATA_LOGGER_CONNECTED_EVENT)
            addAction(ACTION_BATTERY_CHANGED)
            addAction(DATA_LOGGER_ERROR_CONNECT_EVENT)
            addAction(NOTIFICATION_GRAPH_VIEW_TOGGLE)
            addAction(NOTIFICATION_DEBUG_VIEW_TOGGLE)
            addAction(NOTIFICATION_GAUGE_VIEW_TOGGLE)
            addAction(NOTIFICATION_DASH_VIEW_TOGGLE)
            addAction(NOTIFICATION_METRICS_VIEW_TOGGLE)
            addAction(TOGGLE_TOOLBAR_ACTION)
            addAction(SCREEN_OFF_EVENT)
            addAction(SCREEN_ON_EVENT)
        })

        registerReceiver(tripRecorderBroadcastReceiver, IntentFilter().apply {
            addAction(DATA_LOGGER_CONNECTED_EVENT)
            addAction(DATA_LOGGER_STOPPED_EVENT)
        })

        registerReceiver(powerReceiver, IntentFilter().apply {
            addAction("android.intent.action.ACTION_POWER_CONNECTED")
            addAction("android.intent.action.ACTION_POWER_DISCONNECTED")
        })
    }

    private fun setupNavigationBarButtons() {

        val btnStart: FloatingActionButton = findViewById(R.id.connect_btn)
        btnStart.setOnClickListener {
            Log.i(LOGGER_TAG, "Start data logging")
            DataLoggerService.startAction(this)
        }

        val menuButton: FloatingActionButton = findViewById(R.id.menu_btn)
        menuButton.setOnClickListener {
            val pm = PopupMenu(this, menuButton)
            pm.menuInflater.inflate(R.menu.context_menu, pm.menu)

            pm.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.ctx_menu_pids_to_query -> {
                        this@MainActivity.navController()
                            .navigate(
                                R.id.navigation_preferences,
                                bundleOf(PREFERENCE_SCREEN_KEY to "pref.pids.query")
                            )
                    }

                    R.id.ctx_menu_view_profiles -> {
                        this@MainActivity.navController()
                            .navigate(
                                R.id.navigation_preferences,
                                bundleOf(PREFERENCE_SCREEN_KEY to "pref.profiles")
                            )
                    }
                    R.id.ctx_menu_view_configuration -> {

                        val bottomNavigationView: BottomNavigationView = findViewById(R.id.nav_view)
                        val selectedItemId: Int = bottomNavigationView.selectedItemId
                        val currentView: MenuItem =
                            bottomNavigationView.menu.findItem(selectedItemId)

                        val keyToNavigate = when (currentView.itemId) {
                            R.id.navigation_dashboard -> "pref.dashboard"
                            R.id.navigation_gauge -> "pref.gauge"
                            R.id.navigation_graph -> "pref.graph"
                            R.id.navigation_metrics -> "pref.metrics"
                            else -> "pref.root"
                        }

                        this@MainActivity.navController()
                            .navigate(
                                R.id.navigation_preferences,
                                bundleOf(PREFERENCE_SCREEN_KEY to keyToNavigate)
                            )
                    }
                }

                true
            }
            pm.show()
        }
    }
}
