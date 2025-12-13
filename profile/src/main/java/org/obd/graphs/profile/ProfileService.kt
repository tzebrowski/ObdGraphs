 /**
 * Copyright 2019-2025, Tomasz Żebrowski
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

import android.content.SharedPreferences
import android.util.Log
import org.obd.graphs.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val LOG_TAG = "ProfileService"
private const val PROFILE_NAME_PREFIX = "pref.profile.names"
private const val PROFILE_CURRENT_NAME_PREF = "pref.profile.current_name"
private const val INSTALLATION_KEY_PREFIX = "prefs.installed.profiles"
private const val DEFAULT_MAX_PROFILES = 20
private const val DEFAULT_PROFILE = "profile_1"

internal class ProfileService : Profile, SharedPreferences.OnSharedPreferenceChangeListener {

    private val repository = ProfileRepository()
    private val installer = ProfileInstaller(repository)
    private val backupManager = BackupManager(repository)

    @Volatile
    private var isBulkAction = false
    private var versionCode: Int = 0
    private var defaultProfile: String = DEFAULT_PROFILE
    private var versionName: String = ""

    override fun init(versionCode: Int, defaultProfile: String, versionName: String) {
        this.versionCode = versionCode
        this.defaultProfile = defaultProfile
        this.versionName = versionName

        updateBuildSettings()
        repository.registerListener(this)
        Log.i(LOG_TAG, "Profile service initialized. Version=$versionCode")
    }

    // ... [Other methods: updateCurrentProfileName, getAvailableProfiles, etc. unchanged] ...

    override fun updateCurrentProfileName(newName: String) {
        val key = "$PROFILE_NAME_PREFIX.${getCurrentProfile()}"
        repository.update(key, newName)
        updateCurrentProfileNameCache(newName)
    }

    override fun getAvailableProfiles(): Map<String, String?> {
        return (1..DEFAULT_MAX_PROFILES).associate { id ->
            val key = "profile_$id"
            key to repository.getString("$PROFILE_NAME_PREFIX.$key", "Profile $id")
        }
    }

    override fun getCurrentProfile(): String =
        repository.getString(PROFILE_ID_PREF, defaultProfile)

    override fun getCurrentProfileName(): String =
        repository.getString("$PROFILE_NAME_PREFIX.${getCurrentProfile()}", "")

    override fun setupProfiles(forceOverrideRecommendation: Boolean)  =
        try {
            isBulkAction = true
            val installationKey = "$INSTALLATION_KEY_PREFIX.$versionCode"
            installer.install(forceOverrideRecommendation, installationKey, defaultProfile)
            updateBuildSettings()
        } finally {
            isBulkAction = false
        }


    override fun saveCurrentProfile() {
        try {
            isBulkAction = true
            val currentProfile = getCurrentProfile()
            val updates = mutableMapOf<String, Any?>()

            repository.getAll()
                .filterKeys { !isProfileSpecific(it) }
                .forEach { (key, value) ->
                    updates["$currentProfile.$key"] = value
                }

            repository.updateBatch(updates)
            Log.i(LOG_TAG, "Saved profile: $currentProfile")
        } finally {
            isBulkAction = false
        }
    }

    override fun loadProfile(profileName: String)  = try {
            isBulkAction = true
            Log.i(LOG_TAG, "Loading profile: $profileName")

            val updates = mutableMapOf<String, Any?>()
            val allPrefs = repository.getAll()

            allPrefs.keys
                .filter { !isProfileSpecific(it) }
                .forEach { repository.remove(it) }

            allPrefs.forEach { (key, value) ->
                if (key.startsWith("$profileName.")) {
                    val realKey = key.removePrefix("$profileName.")
                    updates[realKey] = value
                }
            }

            repository.updateBatch(updates)
            updateCurrentProfileNameCache(profileName)
            sendBroadcastEvent(PROFILE_CHANGED_EVENT)
        } finally {
            isBulkAction = false
        }


    override fun reset()  = try {
            isBulkAction = true
            repository.update("$INSTALLATION_KEY_PREFIX.$versionCode", false)
            setupProfiles(forceOverrideRecommendation = true)
            sendBroadcastEvent(PROFILE_RESET_EVENT)
        } finally {
            isBulkAction = false
        }


    override fun exportBackup(): File? = backupManager.export()

    override fun restoreBackup() = restoreBackup(File(""))

    override fun restoreBackup(file: File) {
        try {
            isBulkAction = true
            Log.i(LOG_TAG, "Restoring backup from file: ${file.absolutePath}")

            val props = if (file.exists()) backupManager.restore(file) else backupManager.restore()

            val installationKey = "$INSTALLATION_KEY_PREFIX.$versionCode"

            installer.installFromProperties(props, installationKey, defaultProfile)

            sendBroadcastEvent(PROFILE_CHANGED_EVENT)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Restore failed", e)
        } finally {
            isBulkAction = false
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (!isBulkAction && key != null && !isProfileSpecific(key)) {
            val currentProfile = getCurrentProfile()
            val value = repository.getAll()[key]
            Log.d("ProfileAutoSaver", "Auto-saving $key to $currentProfile")
            repository.update("$currentProfile.$key", value)
        }
    }

    private fun updateCurrentProfileNameCache(name: String? = null) {
        val currentName = name ?: repository.getString("$PROFILE_NAME_PREFIX.${getCurrentProfile()}", "")
        repository.update(PROFILE_CURRENT_NAME_PREF, currentName)
    }

    private fun updateBuildSettings() {
        runAsync {
            val format = SimpleDateFormat("yyyyMMdd.HHmm", Locale.getDefault())
            val timestamp = try { format.format(Date()) } catch(e: Exception) { "" }
            repository.update("pref.about.build_time", timestamp)
            repository.update("pref.about.build_version", versionCode.toString())
        }
    }

    private fun isProfileSpecific(key: String): Boolean =
        key.startsWith("profile_") ||
                key.startsWith(PROFILE_NAME_PREFIX) ||
                key == PROFILE_ID_PREF ||
                key.startsWith(INSTALLATION_KEY_PREFIX)
}
