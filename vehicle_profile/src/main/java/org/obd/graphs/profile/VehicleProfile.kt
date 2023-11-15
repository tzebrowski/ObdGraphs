package org.obd.graphs.profile

import android.content.SharedPreferences

interface VehicleProfile : SharedPreferences.OnSharedPreferenceChangeListener {
    fun getProfiles(): Map<String, String?>
    fun getCurrentProfile(): String
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