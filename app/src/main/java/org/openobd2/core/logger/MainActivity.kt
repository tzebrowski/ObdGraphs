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
import android.view.Gravity
import android.view.View
import android.view.WindowManager
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


private const val LOGGER_TAG = "DATA_LOGGER_UI"

class MainActivity : AppCompatActivity() {

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                TOGGLE_TOOLBAR_ACTION -> {
                    if (Preferences.isEnabled(context!!, "pref.toolbar.hide.doubleclick")) {
                        val layout: CoordinatorLayout = findViewById(R.id.coordinator_Layout)
                        layout.isVisible = !layout.isVisible
                    }
                }
                SCREEN_OFF -> {
                   changeScreenBrightness(0f)
//                    lock()
                }
                SCREEN_ON -> {
                    changeScreenBrightness(1f)
                }
                NOTIFICATION_ERROR_CONNECT_BT -> {
                    toast("Error occurred during. Please check your Bluetooth Connection settings.")
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
                        Log.i(LOGGER_TAG, "Stop data logging ")
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
                Log.i(LOGGER_TAG, "Stop data logging ")
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

        if (Preferences.isEnabled(this.applicationContext, "pref.toolbar.hide.landscape")) {
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

        loadPreferences()
        registerReceiver()

        val progressBar: ProgressBar = findViewById(R.id.p_bar)
        progressBar.visibility = View.GONE

        val btnStart: FloatingActionButton = findViewById(R.id.action_btn)
        btnStart.setOnClickListener(View.OnClickListener {
            Log.i(LOGGER_TAG, "Start data logging")
            DataLoggerService.startAction(this)
        })
    }


    override fun onResume() {
        super.onResume()
        registerReceiver()
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
            Preferences.isEnabled(this, "pref.debug.view.enabled")

        findViewById<BottomNavigationView>(R.id.nav_view).menu.findItem(R.id.navigation_dashboard).isVisible =
            Preferences.isEnabled(this, "pref.dash.view.enabled")

        findViewById<BottomNavigationView>(R.id.nav_view).menu.findItem(R.id.navigation_gauge).isVisible =
            Preferences.isEnabled(this, "pref.gauge.view.enabled")

        findViewById<BottomNavigationView>(R.id.nav_view).menu.findItem(R.id.navigation_metrics).isVisible =
            Preferences.isEnabled(this, "pref.metrics.view.enabled")
    }


    private fun registerReceiver() {

        registerReceiver(broadcastReceiver, IntentFilter().apply {
            addAction(NOTIFICATION_CONNECTING)
            addAction(NOTIFICATION_STOPPED)
            addAction(NOTIFICATION_STOPPING)
            addAction(NOTIFICATION_ERROR)
            addAction(NOTIFICATION_CONNECTED)
            addAction(ACTION_BATTERY_CHANGED)
            addAction(NOTIFICATION_ERROR_CONNECT_BT)
            addAction(NOTIFICATION_DEBUG_VIEW_SHOW)
            addAction(NOTIFICATION_DEBUG_VIEW_HIDE)
            addAction(NOTIFICATION_GAUGE_VIEW_SHOW)
            addAction(NOTIFICATION_GAUGE_VIEW_HIDE)
            addAction(NOTIFICATION_DASH_VIEW_SHOW)
            addAction(NOTIFICATION_DASH_VIEW_HIDE)
            addAction(TOGGLE_TOOLBAR_ACTION)
            addAction(NOTIFICATION_METRICS_VIEW_SHOW)
            addAction(NOTIFICATION_METRICS_VIEW_HIDE)
            addAction(SCREEN_OFF)
            addAction(SCREEN_ON)
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
}
