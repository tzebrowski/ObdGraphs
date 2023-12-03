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
package org.obd.graphs.profile

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import androidx.core.content.edit
import org.obd.graphs.*
import org.obd.graphs.preferences.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val LOG_TAG = "VehicleProfile"
private const val PROFILE_AUTO_SAVER_LOG_TAG = "VehicleProfileAutoSaver"
private const val PROFILE_CURRENT_NAME_PREF = "pref.profile.current_name"
private const val PROFILE_INSTALLATION_KEY = "prefs.installed.profiles"
private const val PROFILE_NAME_PREFIX = "pref.profile.names"
private const val DEFAULT_MAX_PROFILES = 20
private const val BACKUP_FILE_NAME = "obd_graphs.backup"
private const val DEFAULT_PROFILE = "profile_1"

internal class PreferencesBackend : Profile {

    private var versionCode: Int = 0
    private var defaultProfile: String? = null
    private lateinit var versionName: String

    private var resources = mutableMapOf<String,String>()

    @Volatile
    private var bulkActionEnabled = false

    override fun updateCurrentProfileName(newName: String) {
        Prefs.edit()
            .putString("$PROFILE_NAME_PREFIX.${getCurrentProfile()}", newName)
            .apply()
    }

    override fun getAvailableProfiles() =
        (1..DEFAULT_MAX_PROFILES)
            .associate {
                "profile_$it" to Prefs.getString(
                    "$PROFILE_NAME_PREFIX.profile_$it",
                    "Profile $it"
                )
            }

    override fun getCurrentProfile(): String = Prefs.getS(PROFILE_ID_PREF, defaultProfile ?: DEFAULT_PROFILE)

    override fun getCurrentProfileName(): String = Prefs.getS("$PROFILE_NAME_PREFIX.${getCurrentProfile()}", "")

    override fun getResources(): Map<String, String> = resources


    override fun importBackup() {
        runAsync {
            try {

                Log.i(LOG_TAG, "Start importing backup file")
                val backupFile = getBackupFile()

                loadProfileFilesIntoPreferences(
                    forceOverride = true,
                    files = mutableListOf(backupFile.absolutePath),
                    installationKey = getInstallationVersion()
                ) {
                    val prop = Properties()
                    prop.load(FileInputStream(it))
                    prop
                }

                Log.i(LOG_TAG, "Exporting backup file completed")

                sendBroadcastEvent(PROFILE_CHANGED_EVENT)

            } catch (e: Throwable) {
                Log.e(LOG_TAG, "Failed to load backup file", e)
            } finally {
                bulkActionEnabled = false
            }
        }
    }

    override fun exportBackup() {
        runAsync {
            try {
                Log.i(LOG_TAG, "Start exporting backup file")
                val data = createExportBackupData()
                val backupFile = getBackupFile()
                data.store(FileOutputStream(backupFile), "Backup file")
                Log.i(LOG_TAG, "Exporting backup file completed")

            } catch (e: Throwable) {
                Log.e(LOG_TAG, "Failed to store backup file", e)
            } finally {
                bulkActionEnabled = false
            }
        }
    }

    override fun reset() {
        try {
            bulkActionEnabled = true
            Prefs.updateBoolean(getInstallationVersion(), false)
            resetCurrentProfile()
            updateBuildSettings()
            setupProfiles(forceOverrideRecommendation = true)
            sendBroadcastEvent(PROFILE_RESET_EVENT)
        } finally {
            bulkActionEnabled = false
        }
    }

    override fun onSharedPreferenceChanged(ss: SharedPreferences?, pref: String?) {
        if (!bulkActionEnabled) {
            pref?.let {
                Log.d(PROFILE_AUTO_SAVER_LOG_TAG, "Receive preference change: $pref")
                if (pref.startsWith("profile_") || pref == getInstallationVersion()) {
                    Log.v(PROFILE_AUTO_SAVER_LOG_TAG, "Skipping: $pref")
                } else {
                    val profileName = getCurrentProfile()
                    ss?.edit {
                        val value = ss.all[pref]
                        Log.d(PROFILE_AUTO_SAVER_LOG_TAG, "Saving: '$profileName.$pref'=$value")
                        updatePreference("$profileName.$pref", value)
                    }
                }
            }
        }
    }

    override fun init(versionCode: Int, defaultProfile: String, versionName: String) {
        Log.i(LOG_TAG,"Profile init, versionCode: $versionCode, defaultProfile: $defaultProfile, versionName: $versionName")
        this.versionCode = versionCode
        this.defaultProfile = defaultProfile
        this.versionName = versionName

        updateBuildSettings()
    }

    override fun setupProfiles(forceOverrideRecommendation: Boolean) {

        try {
            var forceOverride = forceOverrideRecommendation

            val installationKeys = Prefs.all.filterKeys { it.startsWith("prefs.installed.profiles") }.keys.toList()
            Log.i(LOG_TAG, "Found installation keys:  $installationKeys ")

            if (installationKeys.isEmpty()) {
                Log.i(LOG_TAG, "Application is not installed yet.")
                forceOverride = true
            }

            bulkActionEnabled = true
            val installationVersion = getInstallationVersion()
            val installationVersionAvailable = Prefs.getBoolean(installationVersion, false)

            Log.i(LOG_TAG,"Setup profiles. Installation version='$installationVersion', " +
                        "installationKeyAvailable='$installationVersionAvailable', " +
                        "forceOverride=$forceOverride"
            )

            if (!installationVersionAvailable) {
                val profiles = findProfileFiles()
                Log.i(LOG_TAG, "Found following profiles: $profiles for installation.")

                loadProfileFilesIntoPreferences(forceOverride, profiles, installationVersion) {
                    loadFile(it)
                }

                if (forceOverride) {
                    val defaultProfile = getDefaultProfile()
                    Log.i(LOG_TAG, "Setting default profile to: $defaultProfile")
                    loadProfile(getDefaultProfile())
                }
            }

            updateBuildSettings()

            loadResources()

        } finally {
            bulkActionEnabled = false
        }
    }


    override fun saveCurrentProfile() {
        try {
            bulkActionEnabled = true
            Prefs.edit().let {
                val profileName = getCurrentProfile()
                Log.i(LOG_TAG, "Saving user preference to profile='$profileName'")
                Prefs.all
                    .filter { (pref, _) -> !pref.startsWith("profile_") }
                    .filter { (pref, _) -> !pref.startsWith(PROFILE_NAME_PREFIX) }
                    .filter { (pref, _) -> !pref.startsWith(PROFILE_CURRENT_NAME_PREF) }
                    .filter { (pref, _) -> !pref.startsWith(getInstallationVersion()) }
                    .forEach { (pref, value) ->
                        Log.i(LOG_TAG, "'$profileName.$pref'=$value")
                        it.updatePreference("$profileName.$pref", value)
                    }
                it.apply()
            }
        } finally {
            bulkActionEnabled = false
        }
    }

    override fun loadProfile(profileName: String) {
        try {
            bulkActionEnabled = true
            Log.i(LOG_TAG, "Loading user preferences from the profile='$profileName'")

            resetCurrentProfile()

            Prefs.edit().let {
                Prefs.all
                    .filter { (pref, _) -> pref.startsWith(profileName) }
                    .filter { (pref, _) -> !pref.startsWith(PROFILE_NAME_PREFIX) }
                    .filter { (pref, _) -> !pref.startsWith(PROFILE_CURRENT_NAME_PREF) }
                    .filter { (pref, _) -> !pref.startsWith(getInstallationVersion()) }
                    .forEach { (pref, value) ->
                        pref.substring(profileName.length + 1).run {
                            Log.d(LOG_TAG, "Loading user preference $this = $value")
                            it.updatePreference(this, value)
                        }
                    }
                it.apply()
            }

            updateCurrentProfileValue(profileName)
            sendBroadcastEvent(PROFILE_CHANGED_EVENT)
        } finally {
            bulkActionEnabled = false
        }
    }

    private fun resetCurrentProfile() {

        Prefs.edit().let {
            getAvailableModes().forEach { key ->
                it.putString("$MODE_HEADER_PREFIX.$key", "")
                it.putString("$MODE_NAME_PREFIX.$key", "")
            }
            it.putString(PREF_CAN_HEADER_EDITOR, "")
            it.putString(PREF_ADAPTER_MODE_ID_EDITOR, "")

            Prefs.all
                .filter { (pref, _) -> !pref.startsWith("pref.about") }
                .filter { (pref, _) -> !pref.startsWith("datalogger") }
                .filter { (pref, _) -> !pref.startsWith("profile_") }
                .filter { (pref, _) -> !pref.startsWith(PROFILE_ID_PREF) }
                .filter { (pref, _) -> !pref.startsWith(PROFILE_NAME_PREFIX) }
                .filter { (pref, _) -> !pref.startsWith(PROFILE_CURRENT_NAME_PREF) }
                .filter { (pref, _) -> !pref.startsWith(getInstallationVersion()) }
                .forEach { (pref, _) ->
                    it.remove(pref)
                }
            it.apply()
        }
    }

    private fun getInstallationVersion() = "${PROFILE_INSTALLATION_KEY}.${versionCode}"

    private fun updateCurrentProfileValue(profileName: String) {
        val prefName =
            Prefs.getString("$PROFILE_NAME_PREFIX.$profileName", profileName.toCamelCase())
        Log.i(LOG_TAG, "Setting $PROFILE_CURRENT_NAME_PREF=$prefName")
        Prefs.edit().putString(PROFILE_CURRENT_NAME_PREF, prefName).apply()
    }

    private fun stringToStringSet(value: String): MutableSet<String> = value
        .replace("[", "")
        .replace("]", "")
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toMutableSet()

    private fun findProfileFiles(): List<String>? = getContext()!!.assets.list("")?.filter { it.endsWith("properties") }

    private fun loadFile(fileName: String): Properties {
        val prop = Properties()
        prop.load(getContext()!!.assets.open(fileName))
        return prop
    }

    private fun getDefaultProfile(): String = defaultProfile ?: DEFAULT_PROFILE

    @SuppressLint("DefaultLocale")
    private fun loadResources(){
        val entries = Prefs.all
        val keys = entries.keys.filter { it.contains("pref.pids.registry.list") }.toList()
        val values = keys.map { entries[it].toString().replace("[","").replace("]","").split(",") }.flatten().toSet()
        val resourcesMap =  values.map { it.replace(" ", "") to
                it.replace(".json","")
                    .replace("_"," ")
                    .trim()
                    .split(" ").joinToString(" ") { it.toLowerCase().capitalize() }}

        resources.putAll(resourcesMap)
    }

    private fun loadProfileFilesIntoPreferences(
        forceOverride: Boolean,
        files: List<String>?,
        installationKey: String,
        func: (p: String) -> Properties
    ) {
        val allPrefs = Prefs.all

        Prefs.edit().let { editor ->
            if (forceOverride) {
                Log.i(LOG_TAG, "Removing all preferences.")
                // clear all preferences
                editor.clear()
            }
            files?.forEach { profileFile ->
                Log.i(LOG_TAG, "Loading profile file='$profileFile'")

                func(profileFile).forEach { t, u ->
                    val value = u.toString()
                    val key = t.toString()

                    if (forceOverride || !allPrefs.keys.contains(key)) {
                        Log.i(LOG_TAG, "Updating profile.key=`$key=$value`")

                        when {
                            value.isBoolean() -> {
                                editor.putBoolean(key, value.toBoolean())
                            }
                            value.isArray() -> {
                                editor.putStringSet(key, stringToStringSet(value))
                            }
                            value.isNumeric() -> {
                                editor.putInt(key, value.toInt())
                            }
                            else -> {
                                editor.putString(key, value.replace("\"", "").replace("\"", ""))
                            }
                        }
                    } else {
                        Log.i(LOG_TAG, "Skipping profile.key=`$key=$value`")
                    }
                }
            }

            if (forceOverride) {
                Log.i(LOG_TAG, "Updating selected profile to the default=${getDefaultProfile()}")
                editor.putString(PROFILE_ID_PREF, getDefaultProfile())
            }

            Log.i(LOG_TAG, "Updating profile installation key $installationKey to true")
            editor.putBoolean(installationKey, true)
            editor.apply()
        }
    }

    private fun getBackupFile(): File =
        File(getContext()!!.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), BACKUP_FILE_NAME)


    private fun createExportBackupData(): Properties {
        val data = Properties()
        val mm = mutableMapOf<String, String>()

        Prefs.all.forEach {
            when (it.value) {
                is String -> {
                    mm[it.key] = "\"${it.value}\""
                }
                is Boolean -> {
                    mm[it.key] = it.value.toString()
                }
                is Int -> {
                    mm[it.key] = it.value.toString()
                }
                is Set<*> -> {
                    mm[it.key] = (it.value as Set<*>).toString()
                }
                else -> {
                    Log.e(LOG_TAG, "Unknown type for key ${it.key}, skipping")
                }
            }
        }

        data.putAll(mm)
        return data
    }

    private fun updateBuildSettings(){
        runAsync {
            val buildTime = "${SimpleDateFormat("yyyyMMdd.HHmm", Locale.getDefault()).parse(versionName)}"
            Log.i(LOG_TAG,"Update build settings, build time=$buildTime, versionCode=$versionCode")

            Prefs.updateString("pref.about.build_time", buildTime).commit()
            Prefs.updateString("pref.about.build_version", "$versionCode").commit()
            Prefs.updateBoolean("pref.debug.logging.enabled", false).commit()
        }
    }
}