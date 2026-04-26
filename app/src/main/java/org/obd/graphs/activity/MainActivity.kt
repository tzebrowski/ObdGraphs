/*
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
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import org.obd.graphs.BuildConfig
import org.obd.graphs.ExceptionHandler
import org.obd.graphs.MAIN_ACTIVITY_EVENT_DESTROYED
import org.obd.graphs.MAIN_ACTIVITY_EVENT_PAUSE
import org.obd.graphs.Network
import org.obd.graphs.Permissions
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.AutoConnect
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.drag.dragRacingMetricsProcessor
import org.obd.graphs.bl.extra.vehicleStatusMetricsProcessor
import org.obd.graphs.bl.gps.gpsMetricsEmitter
import org.obd.graphs.bl.trip.tripManager
import org.obd.graphs.integrations.gcp.gdrive.TripLogDriveManager
import org.obd.graphs.language.LanguageManager
import org.obd.graphs.preferences.setPreferencesContext
import org.obd.graphs.profile.profile
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.setActivityContext
import org.obd.graphs.ui.BackupManager
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.withDataLogger
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

const val LOG_TAG = "MainActivity"

class MainActivity :
    AppCompatActivity(),
    EasyPermissions.PermissionCallbacks {

    internal lateinit var statusPanel: StatusPanel
    internal val screenLockManager = ScreenLockManager(this)
    internal lateinit var backupManager: BackupManager

    internal lateinit var tripLogDriveManager: TripLogDriveManager

    internal lateinit var appBarConfiguration: AppBarConfiguration

    val drawerLayout: DrawerLayout by lazy { findViewById(R.id.drawer_layout) }

    private var isAppReady = false

    internal var activityBroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                receive(intent)
            }
        }

    override fun attachBaseContext(newBase: Context) {
        val localizedContext = LanguageManager.getLocalizedContext(newBase)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupStrictMode()
        setActivityContext(this)
        setPreferencesContext(this)

        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !isAppReady }

        runWizardFlow()

        setContentView(R.layout.activity_main)

        screen.setupWindowManager(this)
        this.appBarConfiguration = getAppBarConfiguration()

        setupNavigationBar()
        setupBottomBarNavigation()
        setupNavigationViewNavigation()
        setupBackPressHandling()
        registerReceiver()
        setupStatusPanel()
        setupProgressBar()

        screenLockManager.setup()
        lifecycle.addObserver(screenLockManager)

        supportActionBar?.hide()
        backupManager = BackupManager(this)
        tripLogDriveManager = TripLogDriveManager.instance(getString(R.string.ANDROID_WEB_CLIENT_ID), activity = this, null)

        setupFabButtons()

        if (savedInstanceState == null) {
            setupExceptionHandler()
            setupVehicleProfiles()
            Network.setupConnectedNetworksCallback()
            setupMetricsProcessors()
            displayAppSignature(this)
            navigateToLastVisitedScreen()
            AutoConnect.schedule(this)
        }

        isAppReady = true
    }

    private fun setupStatusPanel() {
        statusPanel = StatusPanel(this)
        statusPanel.setup()
    }

    private fun setupFabButtons() {
        FabButtons.setupSpeedDialView(this)

        if (DataLoggerRepository.isRunning()) {
            val connectBtn = FabButtons.view(this).connectFab

            connectBtn.setOnClickListener {
                if (DataLoggerRepository.isRunning()) {
                    withDataLogger {
                        Log.i("Fragment", "Stop data logging")
                        stop()
                    }
                }
            }

            connectBtn.backgroundTintList =
                ContextCompat.getColorStateList(
                    this,
                    if (DataLoggerRepository.isRunning()) {
                        org.obd.graphs.commons.R.color.cardinal
                    } else {
                        org.obd.graphs.commons.R.color.philippine_green
                    }
                )
        }
    }

    override fun onResume() {
        super.onResume()
        screen.setupWindowManager(this)
        screen.changeScreenBrightness(this, 1f)
    }

    override fun onPause() {
        super.onPause()
        sendBroadcastEvent(MAIN_ACTIVITY_EVENT_PAUSE)
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (NavigationRouter.getPreferences().hideToolbarLandscape) {
            val hide = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
            Toolbar.hide(this, hide)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        return NavigationUI.navigateUp(
            navController,
            appBarConfiguration
        ) || super.onSupportNavigateUp()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(
        requestCode: Int,
        perms: MutableList<String>
    ) {
        runWizardFlow()
    }

    override fun onPermissionsDenied(
        requestCode: Int,
        perms: MutableList<String>
    ) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    private fun setupExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler())
    }

    fun getAppBarConfiguration(): AppBarConfiguration =
        AppBarConfiguration(
            setOf(
                R.id.nav_giulia,
                R.id.nav_graph,
                R.id.nav_gauge
            ),
            drawerLayout
        )

    private fun setupStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                ThreadPolicy
                    .Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build()
            )

            StrictMode.setVmPolicy(
                VmPolicy
                    .Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
    }

    private fun setupVehicleProfiles() {
        profile.init(
            versionCode = BuildConfig.VERSION_CODE,
            defaultProfile = resources.getString(R.string.DEFAULT_PROFILE),
            versionName = BuildConfig.VERSION_NAME
        )

        profile.setupProfiles(forceOverrideRecommendation = false)
    }

    private fun setupMetricsProcessors() {
        DataLoggerRepository
            .observe(dragRacingMetricsProcessor)
            .observe(tripManager)
            .observe(vehicleStatusMetricsProcessor)
            .observe(gpsMetricsEmitter)
    }

    private fun runWizardFlow() {
        if (!LanguageManager.isLanguageSelected(this)) {
            LanguageManager.showLanguageSelectionDialog(this) { localeTag ->
                DataLoggerRepository.updateTranslations(localeTag)
                recreate()
            }
            return
        }

        if (Permissions.isAnyPermissionMissing(this)) {
            Permissions.showPermissionOnboarding(this, onDeclined = {
            })
            return
        }
    }

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        )
    }

    private fun setupProgressBar() {
        if (DataLoggerRepository.isRunning()) {
            progressBar {
                it.visibility = View.VISIBLE
                it.indeterminateDrawable.colorFilter =
                    PorterDuffColorFilter(
                        COLOR_PHILIPPINE_GREEN,
                        PorterDuff.Mode.SRC_IN
                    )
            }
        } else {
            progressBar {
                it.visibility = View.GONE
            }
        }
    }
}
