/**
 * Copyright 2019-2023, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
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
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.obd.graphs.*
import org.obd.graphs.preferences.*
import org.obd.graphs.profile.vehicleProfile
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions


const val LOG_TAG = "MainActivity"
class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    lateinit var lockScreenDialog: AlertDialog

    internal var activityBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            receive(intent)
        }
    }
    private val cache: MutableMap<String, Any> = mutableMapOf()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        toolbar { a, b, c ->
            if (getMainActivityPreferences().hideToolbarLandscape) {
                val hide = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                toolbarAnimate(a, b, c, hide)
            }
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

    override fun onCreate(savedInstanceState: Bundle?) {
        setupStrictMode()
        super.onCreate(savedInstanceState)

        setActivityContext(this)
        initCache()
        setContentView(R.layout.activity_main)

        setupWindowManager()
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
        sendBroadcastEvent(MAIN_ACTIVITY_EVENT_DESTROYED)
    }

    private fun setupExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler())
    }

    fun toolbarAnimate(
        bottomNavigationView: BottomNavigationView,
        bottomAppBar: BottomAppBar,
        floatingActionButton: FloatingActionButton,
        hide: Boolean
    ) {
        val bottomNavigationViewHeight: Float = if (hide) bottomNavigationView.height.toFloat() else 0f
        val bottomAppBarHeight: Float = if (hide) bottomAppBar.height.toFloat() else 0f
        val floatingActionButtonHeight: Float = if (hide) 2 * bottomAppBar.height.toFloat() else 0f

        val duration = 200L
        bottomAppBar.clearAnimation()
        bottomAppBar.animate().translationY(bottomAppBarHeight).duration = duration

        bottomNavigationView.clearAnimation()
        bottomNavigationView.animate().translationY(bottomNavigationViewHeight).duration = duration

        floatingActionButton.clearAnimation()
        floatingActionButton.animate().translationY(floatingActionButtonHeight).duration = duration
    }

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
                ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build()
            )

            StrictMode.setVmPolicy(VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build())
        }
    }

    private fun setupVehicleProfiles() {
        vehicleProfile.init(
            versionCode = BuildConfig.VERSION_CODE,
            defaultProfile = resources.getString(R.string.DEFAULT_PROFILE),
            versionName = BuildConfig.VERSION_NAME
        )

        Prefs.registerOnSharedPreferenceChangeListener(vehicleProfile)
        vehicleProfile.setupProfiles(forceOverrideRecommendation = false)
    }
}
