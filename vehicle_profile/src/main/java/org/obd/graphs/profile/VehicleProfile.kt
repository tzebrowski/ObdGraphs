package org.obd.graphs.profile

import android.content.SharedPreferences

const val PROFILE_CHANGED_EVENT = "data.logger.profile.changed.event"
const val PROFILE_RESET_EVENT = "data.logger.profile.reset.event"

interface VehicleProfile : SharedPreferences.OnSharedPreferenceChangeListener {
    fun getAvailableProfiles(): Map<String, String?>
    fun getCurrentProfile(): String
    fun getCurrentProfileName(): String
    fun importBackup()
    fun exportBackup()
    fun reset()
    fun init(versionCode: Int, defaultProfile: String)

    fun setupProfiles(forceOverrideRecommendation: Boolean = true)
    fun saveCurrentProfile()
    fun loadProfile(profileName: String)
}