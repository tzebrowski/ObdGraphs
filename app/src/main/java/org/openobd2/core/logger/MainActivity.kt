package org.openobd2.core.logger


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.openobd2.core.logger.bl.NOTIFICATION_COMPLETE
import org.openobd2.core.logger.bl.NOTIFICATION_CONNECTING
import org.openobd2.core.logger.bl.NOTIFICATION_ERROR


class MainActivity : AppCompatActivity() {

    private var broadcastReciever = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                NOTIFICATION_CONNECTING -> {
                    val progressBar: ProgressBar = findViewById(R.id.p_bar)
                    progressBar.visibility = View.VISIBLE
                }

                NOTIFICATION_COMPLETE -> {
                    val progressBar: ProgressBar = findViewById(R.id.p_bar)
                    progressBar.visibility = View.GONE
                }

                NOTIFICATION_ERROR -> {
                    val progressBar: ProgressBar = findViewById(R.id.p_bar)
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_gauge,
                R.id.navigation_debug,
                R.id.navigation_livedata,
                R.id.navigation_configuration
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        registerReciever()

        val progressBar: ProgressBar = findViewById(R.id.p_bar)
        progressBar.visibility = View.GONE

    }

    override fun onResume() {
        super.onResume()
        registerReciever()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReciever)
    }

    private fun registerReciever() {
        registerReceiver(broadcastReciever, IntentFilter().apply {
            addAction(NOTIFICATION_CONNECTING)
            addAction(NOTIFICATION_COMPLETE)
            addAction("data.logger.stopping")
            addAction(NOTIFICATION_ERROR)
        })
    }
}
