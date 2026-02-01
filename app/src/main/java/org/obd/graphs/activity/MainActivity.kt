 /**
 * Copyright 2019-2026, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import org.obd.graphs.BuildConfig
import org.obd.graphs.ExceptionHandler
import org.obd.graphs.MAIN_ACTIVITY_EVENT_DESTROYED
import org.obd.graphs.MAIN_ACTIVITY_EVENT_PAUSE
import org.obd.graphs.Permissions
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.drag.dragRacingMetricsProcessor
import org.obd.graphs.bl.extra.vehicleStatusMetricsProcessor
import org.obd.graphs.bl.generator.MetricsGenerator
import org.obd.graphs.bl.gps.gpsMetricsEmitter
import org.obd.graphs.bl.trip.tripManager
import org.obd.graphs.cacheManager
import org.obd.graphs.network
import org.obd.graphs.profile.profile
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.setActivityContext
import org.obd.graphs.ui.BackupManager
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

const val LOG_TAG = "MainActivity"

class MainActivity :
    AppCompatActivity(),
    EasyPermissions.PermissionCallbacks {

    lateinit var lockScreenDialog: AlertDialog
    internal lateinit var backupManager: BackupManager

    internal var activityBroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                receive(intent)
            }
        }

    private val cache: MutableMap<String, Any> = mutableMapOf()
    internal lateinit var appBarConfiguration: AppBarConfiguration

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(
        requestCode: Int,
        perms: MutableList<String>,
    ) {
    }

    override fun onPermissionsDenied(
        requestCode: Int,
        perms: MutableList<String>,
    ) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (getMainActivityPreferences().hideToolbarLandscape) {
            val hide = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
            toolbarHide(hide)
        }
    }

    override fun onBackPressed() {
        val drawer = getDrawer()
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    fun getDrawer() = findViewById<View>(R.id.drawer_layout) as DrawerLayout

    override fun onPause() {
        super.onPause()
        sendBroadcastEvent(MAIN_ACTIVITY_EVENT_PAUSE)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        return NavigationUI.navigateUp(navController,appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupStrictMode()
        super.onCreate(savedInstanceState)
        setActivityContext(this)
        initCache()
        setContentView(R.layout.activity_main)

        screen.setupWindowManager(this)

        this.appBarConfiguration = getAppBarConfiguration()

        setupNavigationBar()
        setupNavigationBarButtons()
        registerReceiver()

        setupExceptionHandler()
        setupVehicleProfiles()

        setupStatusPanel()
        network.setupConnectedNetworksCallback()

        progressBar {
            it.visibility = View.GONE
        }

        setupLockScreenDialog()
        setupLeftNavigationPanel()
        supportActionBar?.hide()
        setupMetricsProcessors()
        backupManager = BackupManager(this)
        displayAppSignature(this)

        navigateToLastVisitedScreen()
        validatePermissions()
    }


    override fun onResume() {
        super.onResume()
        screen.setupWindowManager(this)
        screen.changeScreenBrightness(this, 1f)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            screen.hideSystemUI(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
        sendBroadcastEvent(MAIN_ACTIVITY_EVENT_DESTROYED)
    }

    private fun setupExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler())
    }


    fun getAppBarConfiguration(): AppBarConfiguration = AppBarConfiguration(
        setOf(
            R.id.nav_giulia,
            R.id.nav_graph,
            R.id.nav_gauge,
        ),
        findViewById<DrawerLayout>(R.id.drawer_layout)
    )

    private fun initCache() {
        cacheManager.initCache(cache)
    }

    private fun setupLockScreenDialog() {
        AlertDialog.Builder(this).run {
            setCancelable(false)

            val dialogView: View = this@MainActivity.layoutInflater.inflate(R.layout.dialog_screen_lock, null)
            setView(dialogView)
            lockScreenDialog = create()
        }
    }

    private fun setupStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                ThreadPolicy
                    .Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build(),
            )

            StrictMode.setVmPolicy(
                VmPolicy
                    .Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
        }
    }

    private fun setupVehicleProfiles() {
        profile.init(
            versionCode = BuildConfig.VERSION_CODE,
            defaultProfile = resources.getString(R.string.DEFAULT_PROFILE),
            versionName = BuildConfig.VERSION_NAME,
        )

        profile.setupProfiles(forceOverrideRecommendation = false)
    }

    private fun setupMetricsProcessors() {
        dataLogger
            .observe(dragRacingMetricsProcessor)
            .observe(tripManager)
            .observe(vehicleStatusMetricsProcessor)
            .observe(gpsMetricsEmitter)

        if (BuildConfig.DEBUG) {
            dataLogger.observe(MetricsGenerator(BuildConfig.DEBUG))
        }
    }

    private fun validatePermissions() {
        if (Permissions.isAnyPermissionMissing(this)) {
            Permissions.showPermissionOnboarding(this)
        }
    }
}
