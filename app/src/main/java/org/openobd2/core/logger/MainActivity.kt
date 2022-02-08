package org.openobd2.core.logger


import android.app.admin.DevicePolicyManager
import android.content.*
import android.content.Intent.ACTION_BATTERY_CHANGED
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.PowerManager
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.util.Log
import android.view.*
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
import org.openobd2.core.logger.ui.common.Cache
import org.openobd2.core.logger.ui.common.TOGGLE_TOOLBAR_ACTION
import org.openobd2.core.logger.ui.preferences.*


private const val LOGGER_TAG = "DATA_LOGGER_UI"

class MainActivity : AppCompatActivity() {

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                TOGGLE_TOOLBAR_ACTION -> {
                    if (Prefs.isEnabled("pref.toolbar.hide.doubleclick")) {
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
                    toast("Error occurred during. Please check your Bluetooth Connection settings.")
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
                        Log.i(LOGGER_TAG, "Stop data logging ")
                        DataLoggerService.stopAction(context!!)
                    })
                    btn.refreshDrawableState()
                }

                DATA_LOGGER_CONNECTED_EVENT -> {
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

                DATA_LOGGER_STOPPED_EVENT -> {
                    toast("Connection with the device has been stopped.")
                    handleStop(context!!)
                }

                DATA_LOGGER_ERROR_EVENT -> {
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
                Log.i(LOGGER_TAG, "Stop data logging ")
                DataLoggerService.startAction(context)
            })
        }

        private fun toggleNavigationItem(id: Int) {
            findViewById<BottomNavigationView>(R.id.nav_view).menu.findItem(id)?.run {
                this.isVisible = !this.isVisible
            }
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

    private val cache: MutableMap<String, Any> = mutableMapOf()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (Prefs.isEnabled("pref.toolbar.hide.landscape")) {
            val layout: CoordinatorLayout = this.findViewById(R.id.coordinator_Layout)
            layout.isVisible = newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val policy = ThreadPolicy.Builder()
            .permitAll().build()
        StrictMode.setThreadPolicy(policy)

        //keeps screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        DataLogger.INSTANCE.init(this.application)

        setContentView(R.layout.activity_main)
        setupNavigation()

        setupPreferences()
        registerReceiver()

        val progressBar: ProgressBar = findViewById(R.id.p_bar)
        progressBar.visibility = View.GONE

        val btnStart: FloatingActionButton = findViewById(R.id.action_btn)
        btnStart.setOnClickListener(View.OnClickListener {
            Log.i(LOGGER_TAG, "Start data logging")
            DataLoggerService.startAction(this)
        })
        setupWindowManager()
        Cache  = cache
    }

    override fun onResume() {
        super.onResume()
        registerReceiver()
        setupWindowManager()
        changeScreenBrightness(1f)
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
                R.id.navigation_gauge,
                R.id.navigation_graph,
                R.id.navigation_dashboard,
                R.id.navigation_debug,
                R.id.navigation_metrics,
                R.id.navigation_configuration
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private fun setupPreferences() {
        Prefs = PreferenceManager.getDefaultSharedPreferences(this)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        findViewById<BottomNavigationView>(R.id.nav_view).menu.run{
            findItem(R.id.navigation_debug)?.isVisible =
                Prefs.isEnabled("pref.debug.view.enabled")

            findItem(R.id.navigation_dashboard).isVisible =
                Prefs.isEnabled("pref.dash.view.enabled")

            findItem(R.id.navigation_gauge).isVisible =
                Prefs.isEnabled("pref.gauge.view.enabled")

            findItem(R.id.navigation_metrics).isVisible =
                Prefs.isEnabled("pref.metrics.view.enabled")

            findItem(R.id.navigation_graph).isVisible =
                Prefs.isEnabled("pref.graph.view.enabled")
       }
    }

    private fun registerReceiver() {

        registerReceiver(broadcastReceiver, IntentFilter().apply {
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
    }

    private fun changeScreenBrightness(value: Float) {
        try {

            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "data_logger:wakeLock"
            )
            wl.acquire()
            val params: WindowManager.LayoutParams =
                window.attributes
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            params.screenBrightness = value
            window.attributes = params
            wl.release()
        } catch (e: Throwable){
            Log.e(LOGGER_TAG, "Failed to change screen brightness", e)
        }
    }

    private fun lockScreen() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isScreenOn) {
            val policy = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
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
                this.startActivity(intent)
            }
        }
    }

    private fun setupWindowManager() {
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
    }
}
