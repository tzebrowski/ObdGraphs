package org.openobd2.core.logger


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.openobd2.core.logger.bl.*


class MainActivity : AppCompatActivity() {

    private var broadcastReciever = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                NOTIFICATION_CONNECTING -> {
                    val toast = Toast.makeText(
                        applicationContext, "Connecting to the device.",
                        Toast.LENGTH_LONG
                    )
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()


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
                    val toast = Toast.makeText(
                        applicationContext, "Connection to the device has been established." +
                                "\n Start collecting data from ECU.",
                        Toast.LENGTH_LONG
                    )
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()

                    val progressBar: ProgressBar = findViewById(R.id.p_bar)
                    progressBar.indeterminateDrawable.setColorFilter(
                        Color.parseColor("#01804F"),
                        android.graphics.PorterDuff.Mode.SRC_IN
                    )
                }

                NOTIFICATION_STOPPED -> {
                    val toast = Toast.makeText(
                        applicationContext, "Connection with the device has been stopped.",
                        Toast.LENGTH_LONG
                    )
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()

                    val progressBar: ProgressBar = findViewById(R.id.p_bar)
                    progressBar.visibility = View.GONE

                    val btn: FloatingActionButton = findViewById(R.id.action_btn)
                    btn.backgroundTintList = resources.getColorStateList(R.color.purple_500)
                    btn.setOnClickListener(View.OnClickListener {
                        Log.i("DATA_LOGGER_UI", "Stop data logging ")
                        DataLoggerService.startAction(context!!)
                    })
                }

                NOTIFICATION_ERROR -> {
                    val toast = Toast.makeText(
                        applicationContext, "Error occurred during. Please check your connection.",
                        Toast.LENGTH_LONG
                    )
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()

                    val progressBar: ProgressBar = findViewById(R.id.p_bar)
                    progressBar.visibility = View.GONE

                    val btn: FloatingActionButton = findViewById(R.id.action_btn)
                    btn.backgroundTintList = resources.getColorStateList(R.color.purple_500)
                    btn.setOnClickListener(View.OnClickListener {
                        Log.i("DATA_LOGGER_UI", "Stop data logging ")
                        DataLoggerService.startAction(context!!)
                    })
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataLoggerService.dataLogger.init(this.application)

        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard,
                R.id.navigation_gauge,
                R.id.navigation_debug,
                R.id.navigation_livedata,
                R.id.navigation_configuration
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        registerReciever()

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
        registerReciever()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReciever)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val decorView = window.decorView
        decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // Hide the nav bar and status bar
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }


    private fun registerReciever() {
        registerReceiver(broadcastReciever, IntentFilter().apply {
            addAction(NOTIFICATION_CONNECTING)
            addAction(NOTIFICATION_STOPPED)
            addAction(NOTIFICATION_STOPPING)
            addAction(NOTIFICATION_ERROR)
            addAction(NOTIFICATION_CONNECTED)
        })
    }
}
