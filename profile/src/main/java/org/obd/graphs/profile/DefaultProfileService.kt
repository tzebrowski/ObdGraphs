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
package org.obd.graphs.profile

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import androidx.core.content.edit
import org.obd.graphs.PREF_DRAG_RACE_KEY_PREFIX
import org.obd.graphs.PREF_MODULE_LIST
import org.obd.graphs.diagnosticRequestIDMapper
import org.obd.graphs.getContext
import org.obd.graphs.modules
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import org.obd.graphs.preferences.updateBoolean
import org.obd.graphs.preferences.updatePreference
import org.obd.graphs.preferences.updateString
import org.obd.graphs.runAsync
import org.obd.graphs.sendBroadcastEvent
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Properties

private const val LOG_TAG = "VehicleProfile"
private const val PROFILE_AUTO_SAVER_LOG_TAG = "VehicleProfileAutoSaver"
private const val PROFILE_CURRENT_NAME_PREF = "pref.profile.current_name"
private const val PROFILE_INSTALLATION_KEY = "prefs.installed.profiles"
private const val PROFILE_NAME_PREFIX = "pref.profile.names"
private const val DEFAULT_MAX_PROFILES = 20
private const val BACKUP_FILE_NAME = "obd_graphs.backup"
private const val DEFAULT_PROFILE = "profile_1"

internal class DefaultProfileService :
    Profile,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private var versionCode: Int = 0
    private var defaultProfile: String? = null
    private lateinit var versionName: String

    @Volatile
    private var bulkActionEnabled = false

    override fun updateCurrentProfileName(newName: String) {
        Prefs
            .edit()
            .putString("$PROFILE_NAME_PREFIX.${getCurrentProfile()}", newName)
            .apply()
    }

    override fun getAvailableProfiles() =
        (1..DEFAULT_MAX_PROFILES)
            .associate {
                "profile_$it" to
                    Prefs.getString(
                        "$PROFILE_NAME_PREFIX.profile_$it",
                        "Profile $it",
                    )
            }

    override fun getCurrentProfile(): String = Prefs.getS(PROFILE_ID_PREF, defaultProfile ?: DEFAULT_PROFILE)

    override fun getCurrentProfileName(): String = Prefs.getS("$PROFILE_NAME_PREFIX.${getCurrentProfile()}", "")

    override fun restoreBackup() {
        restoreBackup(getBackupFile())
    }

    override fun restoreBackup(file: File) {
        try {
            Log.i(LOG_TAG, "Start restoring backup file: ${file.absoluteFile}")

            val prop = Properties()
            file.inputStream().use { stream ->
                prop.load(stream)
            }

            Prefs.edit().let { editor ->
                editor.clear()
                prop.forEach { keyObject, valueObject ->

                    val value = valueObject.toString()
                    val key = keyObject.toString()

                    if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                        Log.d(LOG_TAG, "Restoring profile.key=`$key=$value`")
                    }

                    when {
                        value.isArray() -> editor.putStringSet(key, value.toStringSet())
                        value.isBoolean() -> editor.putBoolean(key, value.toBoolean())
                        value.isNumeric() -> editor.putInt(key, value.toInt())
                        else -> editor.putString(key, value.replace("\"", "").replace("\"", ""))
                    }
                }

                editor.putBoolean(getInstallationVersion(), true)
                editor.apply()
            }

            Log.i(LOG_TAG, "Restoring backup file completed")
            sendBroadcastEvent(PROFILE_CHANGED_EVENT)
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to restore backup file", e)
        } finally {
            bulkActionEnabled = false
        }
    }

    override fun exportBackup(): File? {
        try {
            Log.i(LOG_TAG, "Start exporting backup file")
            val backupFile = getBackupFile()
            val data = createExportBackupData()
            data.store(FileOutputStream(backupFile), "Backup file")
            Log.i(LOG_TAG, "Exporting backup file completed")
            return backupFile
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to store backup file", e)
        } finally {
            bulkActionEnabled = false
        }
        return null
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

    override fun onSharedPreferenceChanged(
        ss: SharedPreferences?,
        pref: String?,
    ) {
        if (!bulkActionEnabled) {
            pref?.let {
                if (Log.isLoggable(PROFILE_AUTO_SAVER_LOG_TAG, Log.DEBUG)) {
                    Log.d(PROFILE_AUTO_SAVER_LOG_TAG, "Receive preference change: $pref")
                }

                if (pref.startsWith("profile_") || pref == getInstallationVersion()) {
                    if (Log.isLoggable(PROFILE_AUTO_SAVER_LOG_TAG,Log.VERBOSE)) {
                        Log.v(PROFILE_AUTO_SAVER_LOG_TAG, "Skipping: $pref")
                    } else {
                        //
                    }
                } else {
                    val profileName = getCurrentProfile()
                    ss?.edit {
                        val value = ss.all[pref]

                        if (Log.isLoggable(PROFILE_AUTO_SAVER_LOG_TAG, Log.VERBOSE)) {
                            Log.v(PROFILE_AUTO_SAVER_LOG_TAG, "Saving: '$profileName.$pref'=$value")
                        }

                        updatePreference("$profileName.$pref", value)
                    }
                }
            }
        }
    }

    override fun init(
        versionCode: Int,
        defaultProfile: String,
        versionName: String,
    ) {
        Log.i(LOG_TAG, "Profile init, versionCode: $versionCode, defaultProfile: $defaultProfile, versionName: $versionName")
        this.versionCode = versionCode
        this.defaultProfile = defaultProfile
        this.versionName = versionName

        updateBuildSettings()
        Prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun setupProfiles(forceOverrideRecommendation: Boolean) {
        try {
            var forceOverride = forceOverrideRecommendation

            val installationKeys =
                Prefs.all
                    .filterKeys { it.startsWith("prefs.installed.profiles") }
                    .keys
                    .toList()
            Log.i(LOG_TAG, "Found installation keys:  $installationKeys ")

            if (installationKeys.isEmpty()) {
                Log.i(LOG_TAG, "Application is not installed yet.")
                forceOverride = true
            }

            bulkActionEnabled = true
            val installationVersion = getInstallationVersion()
            val installationVersionAvailable = Prefs.getBoolean(installationVersion, false)

            Log.i(
                LOG_TAG,
                "Setup profiles. Installation version='$installationVersion', " +
                    "installationKeyAvailable='$installationVersionAvailable', " +
                    "forceOverride=$forceOverride",
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
            allProps().let {
                runAsync {
                    distributePreferences(it)
                }
            }
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
                    .filter { (pref, _) -> pref.startsWith("$profileName.") }
                    .filter { (pref, _) -> !pref.startsWith(PROFILE_NAME_PREFIX) }
                    .filter { (pref, _) -> !pref.startsWith(PROFILE_CURRENT_NAME_PREF) }
                    .filter { (pref, _) -> !pref.startsWith(getInstallationVersion()) }
                    .filter { (pref, _) -> !pref.startsWith(PREF_DRAG_RACE_KEY_PREFIX) }
                    .forEach { (pref, value) ->

                        pref.substring(profileName.length + 1).run {
                            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                                Log.d(LOG_TAG, "Loading user preference for $profileName,  $pref =  $this = $value")
                            }
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
        diagnosticRequestIDMapper.reset()

        Prefs.edit().let {
            Prefs.all
                .filter { (pref, _) -> !pref.startsWith("pref.about") }
                .filter { (pref, _) -> !pref.startsWith("datalogger") }
                .filter { (pref, _) -> !pref.startsWith("profile_") }
                .filter { (pref, _) -> !pref.startsWith(PROFILE_ID_PREF) }
                .filter { (pref, _) -> !pref.startsWith(PROFILE_NAME_PREFIX) }
                .filter { (pref, _) -> !pref.startsWith(PROFILE_CURRENT_NAME_PREF) }
                .filter { (pref, _) -> !pref.startsWith(getInstallationVersion()) }
                .filter { (pref, _) -> !pref.startsWith(PREF_DRAG_RACE_KEY_PREFIX) }
                .forEach { (pref, _) ->
                    it.remove(pref)
                }
            it.apply()
        }
    }

    private fun getInstallationVersion() = "${PROFILE_INSTALLATION_KEY}.$versionCode"

    private fun updateCurrentProfileValue(profileName: String) {
        val prefName =
            Prefs.getString("$PROFILE_NAME_PREFIX.$profileName", profileName.toCamelCase())
        Log.i(LOG_TAG, "Setting $PROFILE_CURRENT_NAME_PREF=$prefName")
        Prefs.edit().putString(PROFILE_CURRENT_NAME_PREF, prefName).apply()
    }


    private fun findProfileFiles(): List<String>? = getContext()!!.assets.list("")?.filter { it.endsWith("properties") }

    private fun loadFile(fileName: String): Properties {
        val prop = Properties()
        prop.load(getContext()!!.assets.open(fileName))
        return prop
    }

    private fun getDefaultProfile(): String = defaultProfile ?: DEFAULT_PROFILE

    @SuppressLint("DefaultLocale")
    private fun distributePreferences(entries: MutableMap<String, Any?>) {
        diagnosticRequestIDMapper.updateSettings(entries)
        modules.updateSettings(entries)
    }

    private fun allProps(): MutableMap<String, Any?> {
        val entries = mutableMapOf<String, Any?>()

        try {
            findProfileFiles()?.forEach {
                val file = loadFile(it)
                for (name in file.stringPropertyNames()) {
                    entries[name] = file.getProperty(name)
                }
            }
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "Failed to load properties files", e)
        }

        if (entries.isEmpty()) {
            entries.putAll(Prefs.all.toMutableMap())
        }
        return entries
    }

    private fun loadProfileFilesIntoPreferences(
        forceOverride: Boolean,
        files: List<String>?,
        installationKey: String,
        func: (p: String) -> Properties,
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

                    if (forceOverride || !allPrefs.keys.contains(key) || allowedToOverride().any { key.contains(it) }) {
                        Log.i(LOG_TAG, "Updating profile.key=`$key=$value`")

                        when {
                            value.isArray() -> {
                                if (key.startsWith(getCurrentProfile())) {
                                    val currentProfilePropName = key.substring(getCurrentProfile().length + 1, key.length)
                                    Log.i(LOG_TAG, "Updating current profile value $currentProfilePropName=$value")
                                    editor.putStringSet(currentProfilePropName, value.toStringSet())
                                }
                                editor.putStringSet(key, value.toStringSet())
                            }

                            value.isBoolean() -> editor.putBoolean(key, value.toBoolean())
                            value.isNumeric() -> editor.putInt(key, value.toInt())
                            else -> editor.putString(key, value.replace("\"", "").replace("\"", ""))
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

    private fun allowedToOverride() = setOf(diagnosticRequestIDMapper.getValuePreferenceName(), PREF_MODULE_LIST)

    private fun getBackupFile(): File = File(getContext()!!.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), BACKUP_FILE_NAME)

    private fun createExportBackupData(): Properties {
        val data = Properties()
        val mm = mutableMapOf<String, String>()

        Prefs.all.forEach {
            if (it.key.contains("..") || it.key.startsWith(".")) {
                Log.e(LOG_TAG, "Skipping invalid key ${it.key}")
                return@forEach
            }
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

    private fun updateBuildSettings() {
        runAsync {
            val buildTime = "${SimpleDateFormat("yyyyMMdd.HHmm", Locale.getDefault()).parse(versionName)}"
            Log.i(LOG_TAG, "Update build settings, build time=$buildTime, versionCode=$versionCode")

            Prefs.updateString("pref.about.build_time", buildTime).commit()
            Prefs.updateString("pref.about.build_version", "$versionCode").commit()
            Prefs.updateBoolean("pref.debug.logging.enabled", false).commit()
        }
    }
}
