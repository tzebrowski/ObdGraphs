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
package org.obd.graphs.preferences.profile

import android.content.SharedPreferences
import android.util.Log
import org.obd.graphs.*
import org.obd.graphs.bl.datalogger.PROFILE_CHANGED_EVENT
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateBoolean
import org.obd.graphs.preferences.updateToolbar
import org.obd.graphs.profile.PROFILE_ID_PREF
import org.obd.graphs.profile.PROFILE_NAME_PREFIX
import org.obd.graphs.profile.getSelectedVehicleProfile
import org.obd.graphs.profile.getVehicleProfiles
import java.util.*

const val LOG_KEY = "VehicleProfile"
const val PROFILES_PREF = "pref.profiles"

private const val PROFILE_CURRENT_NAME_PREF = "pref.profile.current_name"
private const val PROFILE_INSTALLATION_KEY = "prefs.installed.profiles"
private const val DEFAULT_PROFILE = "profile_1"

val vehicleProfile = VehicleProfile()
class VehicleProfile {

    fun reset(){
        Prefs.updateBoolean(getProfileInstallationKey(), false)
        resetCurrentProfile()
        setupProfiles(forceOverride = true)
    }


    fun getProfileList() = getVehicleProfiles()

    fun setupProfiles(forceOverride:Boolean=true) {

        val installationKey = getProfileInstallationKey()
        val setupDisabled = Prefs.getBoolean(installationKey, false)
        Log.i(LOG_KEY, "Setup profiles. Installation key='$installationKey', setupEnabled='$setupDisabled', forceOverride=$forceOverride")

        if (!setupDisabled) {
            val profiles = findProfileFiles()
            Log.i(LOG_KEY, "Found following profiles: $profiles for installation.")
            val allPrefs = Prefs.all

            Prefs.edit().let { editor ->
                if (forceOverride) {
                    Log.i(LOG_KEY, "Removing all preferences.")
                    // clear all preferences
                    editor.clear()
                }

                profiles?.forEach { profileFile ->
                    Log.i(LOG_KEY, "Loading profile file='$profileFile'")

                    openProfileFile(profileFile).forEach { t, u ->
                        val value = u.toString()
                        val key = t.toString()

                        if (forceOverride || !allPrefs.keys.contains(key)) {
                            Log.i(LOG_KEY, "Updating profile.key=`$key=$value`")

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
                        } else{
                            Log.i(LOG_KEY, "Skipping profile.key=`$key=$value`")
                        }

                    }
                }
                editor.putString(PROFILE_ID_PREF, getDefaultProfile())
                Log.i(LOG_KEY,"Updating profile installation key $installationKey to true")
                editor.putBoolean(installationKey, true)
                editor.apply()
            }
            val defaultProfile = getDefaultProfile()
            Log.i(LOG_KEY,"Setting default profile to: $defaultProfile")
            loadProfile(getDefaultProfile())
            updateToolbar()
        }
    }

    internal fun getCurrentProfile(): String = getSelectedVehicleProfile()

    internal fun saveCurrentProfile() {
        Prefs.edit().let {
            val profileName = getCurrentProfile()
            Log.i(LOG_KEY, "Saving user preference to profile='$profileName'")
            Prefs.all
                .filter { (pref, _) -> !pref.startsWith("profile_") }
                .filter { (pref, _) -> !pref.startsWith(PROFILE_NAME_PREFIX) }
                .filter { (pref, _) -> !pref.startsWith(PROFILE_CURRENT_NAME_PREF) }
                .filter { (pref, _) -> !pref.startsWith(getProfileInstallationKey()) }
                .forEach { (pref, value) ->
                    Log.i(LOG_KEY, "'$profileName.$pref'=$value")
                    it.updatePreference("$profileName.$pref", value)
                }
            it.apply()
        }
    }

    fun loadProfile(profileName: String) {
        Log.i(LOG_KEY, "Loading user preferences from the profile='$profileName'")

        resetCurrentProfile()

        Prefs.edit().let {
            Prefs.all
                .filter { (pref, _) -> pref.startsWith(profileName) }
                .filter { (pref, _) -> !pref.startsWith(PROFILE_NAME_PREFIX) }
                .filter { (pref, _) -> !pref.startsWith(PROFILE_CURRENT_NAME_PREF) }
                .filter { (pref, _) -> !pref.startsWith(getProfileInstallationKey()) }
                .forEach { (pref, value) ->
                    pref.substring(profileName.length + 1).run {
                        Log.d(LOG_KEY, "Loading user preference $this = $value")
                        it.updatePreference(this, value)
                    }
                }
            it.apply()
        }

        updateCurrentProfileValue(profileName)
        sendBroadcastEvent(PROFILE_CHANGED_EVENT)
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
                .filter { (pref, _) -> !pref.startsWith(getProfileInstallationKey()) }
                .forEach { (pref, _) ->
                    it.remove(pref)
                }
            it.apply()
        }
    }

    private fun getProfileInstallationKey() = "${PROFILE_INSTALLATION_KEY}.${BuildConfig.VERSION_CODE}"

    private fun SharedPreferences.Editor.updatePreference(
        prefName: String,
        value: Any?
    ) {
        when (value) {
            is String -> {
                putString(prefName, value)
            }
            is Set<*> -> {
                putStringSet(prefName, value as MutableSet<String>?)
            }
            is Int -> {
                putInt(prefName, value)
            }
            is Boolean -> {
                putBoolean(prefName, value)
            }
        }
    }


    private fun updateCurrentProfileValue(profileName: String) {
        val prefName =
            Prefs.getString("$PROFILE_NAME_PREFIX.$profileName", profileName.toCamelCase())
        Log.i(LOG_KEY, "Setting $PROFILE_CURRENT_NAME_PREF=$prefName")
        Prefs.edit().putString(PROFILE_CURRENT_NAME_PREF, prefName).apply()
    }


    private fun String.toCamelCase() =
        split('_').joinToString(" ", transform = String::capitalize)

    private fun String.isArray() = startsWith("[") || endsWith("]")
    private fun String.isBoolean(): Boolean = startsWith("false") || startsWith("true")
    private fun String.isNumeric(): Boolean = matches(Regex("-?\\d+"))
    private fun String.toBoolean(): Boolean = startsWith("true")


    private fun stringToStringSet(value: String): MutableSet<String> = value
        .replace("[", "")
        .replace("]", "")
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toMutableSet()

    private fun findProfileFiles(): List<String>?  =  getContext()!!.assets.list("")?.filter { it.endsWith("properties") }

    private fun openProfileFile(fileName: String): Properties {
        val prop = Properties()
        prop.load(getContext()!!.assets.open(fileName))
        return prop
    }

    private fun getDefaultProfile(): String =
        getContext()?.resources?.getString(R.string.DEFAULT_PROFILE) ?: DEFAULT_PROFILE
}