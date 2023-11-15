package org.obd.graphs.profile

import android.content.SharedPreferences

interface VehicleProfile : SharedPreferences.OnSharedPreferenceChangeListener {
    fun getProfiles(): Map<String, String?>
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