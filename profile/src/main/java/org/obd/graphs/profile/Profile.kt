/**
 * Copyright 2019-2024, Tomasz Żebrowski
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

import android.content.SharedPreferences

const val PROFILE_CHANGED_EVENT = "data.logger.profile.changed.event"
const val PROFILE_RESET_EVENT = "data.logger.profile.reset.event"
const val PROFILES_PREF = "pref.profiles"
const val PROFILE_ID_PREF = "pref.profile.id"

val profile: Profile = ProfilePreferencesBackend()

interface Profile : SharedPreferences.OnSharedPreferenceChangeListener {

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