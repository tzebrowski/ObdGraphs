package org.obd.graphs.profile

import android.content.SharedPreferences

const val PROFILE_CHANGED_EVENT = "data.logger.profile.changed.event"
const val PROFILE_RESET_EVENT = "data.logger.profile.reset.event"
const val PROFILES_PREF = "pref.profiles"
const val PROFILE_ID_PREF = "pref.profile.id"

val vehicleProfile: VehicleProfile = InPreferencesVehicleProfile()

interface VehicleProfile : SharedPreferences.OnSharedPreferenceChangeListener {

    fun updateCurrentProfileName(newName: String)
    fun getAvailableProfiles(): Map<String, String?>
    fun getCurrentProfile(): String
    fun getCurrentProfileName(): String
    fun importBackup()
    fun exportBackup()
    fun reset()
    fun init(versionCode: Int, defaultProfile: String, versionName: String)
    fun setupProfiles(forceOverrideRecommendation: Boolean = true)
    fun saveCurrentProfile()
    fun loadProfile(profileName: String)
}