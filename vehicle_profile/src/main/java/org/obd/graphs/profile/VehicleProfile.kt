package org.obd.graphs.profile

import android.content.SharedPreferences

interface VehicleProfile : SharedPreferences.OnSharedPreferenceChangeListener {
    fun getCurrentProfile(): String
    fun getProfileList(): Map<String, String?>
    fun getProfiles(): Map<String, String?>
    fun getSelectedProfile(): String
    fun getSelectedProfileName(): String?
    fun importBackup()
    fun exportBackup()
    fun reset()
    fun updateVersionCode(code: Int)
    fun updateDefaultProfile(profile: String?)
    fun setupProfiles(forceOverrideRecommendation: Boolean = true)
    fun saveCurrentProfile()
    fun loadProfile(profileName: String)
}