package org.openobd2.core.logger


import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.openobd2.core.logger.bl.DataLoggerService


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val btnStop: Button = findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(View.OnClickListener {
            Log.i("DATA_LOGGER_UI", "Stop data logging ")
            DataLoggerService.stopAction(this)
        });

        val btnStart: Button = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(View.OnClickListener {
            Log.i("DATA_LOGGER_UI", "Start data logging")
            DataLoggerService.startAction(this, "OBDII")
        });
    }
}
