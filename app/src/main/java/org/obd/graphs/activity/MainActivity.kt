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
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import org.obd.graphs.*
import org.obd.graphs.preferences.*
import org.obd.graphs.preferences.profile.vehicleProfile
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.text.SimpleDateFormat
import java.util.*


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

        toolbar {
            if (getMainActivityPreferences().hideToolbarLandscape) {
                it.isVisible = newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE
            }
        }
    }


    override fun onPause() {
        super.onPause()
        sendBroadcastEvent(MAIN_ACTIVITY_EVENT_PAUSE)
    }

    private lateinit var  actionBarDrawerToggle: ActionBarDrawerToggle


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
        vehicleProfile.setupProfiles(forceOverride = false)
        setupStatusPanel()
        setupPreferences()

        network.setupConnectedNetworksCallback()

        progressBar {
            it.visibility = View.GONE
        }

        setupLockScreenDialog()
        Prefs.registerOnSharedPreferenceChangeListener(vehicleProfile)
        setupLeftNavigationPanel()
    }

    private fun setupLeftNavigationPanel() {
        val drawerLayout: DrawerLayout = findViewById(R.id.my_drawer_layout)
        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close)
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        leftAppBar {
            it.setNavigationItemSelectedListener { item ->
                navigateToScreen(item.itemId)
                true
            }
        }
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

    private fun setupPreferences() {
        runAsync {
            Prefs.updateString("pref.about.build_time", "${SimpleDateFormat("yyyyMMdd.HHmm", Locale.getDefault()).parse(BuildConfig.VERSION_NAME)}")
            Prefs.updateString("pref.about.build_version", "${BuildConfig.VERSION_CODE}")
            Prefs.updateBoolean("pref.debug.logging.enabled", false)
        }
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
}
