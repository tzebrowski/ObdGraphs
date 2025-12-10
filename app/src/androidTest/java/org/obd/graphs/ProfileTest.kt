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
package org.obd.graphs

import android.util.Log
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.activity.MainActivity
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateBoolean
import org.obd.graphs.profile.profile

@RunWith(AndroidJUnit4ClassRunner::class)
class ProfileTest {

    @Test
    fun resetProfileTest() {

        launchActivity<MainActivity>().use {

            // lets use this profiles as default
            profile.loadProfile("profile_5")

            val propName = "pref.adapter.batch.enabled"
            assertEquals(Prefs.getBoolean(propName, true), true)

            // changing the value of property under profile_5
            Prefs.updateBoolean("pref.adapter.batch.enabled", false)
            assertEquals(Prefs.getBoolean(propName, false), false)

            // resetting profiles
            profile.reset()
            assertEquals(Prefs.getBoolean(propName, true), true)
        }
    }

    @Test
    fun loadProfileTest() {
        launchActivity<MainActivity>().use {
            assertProfilesExists()
        }
    }

    @Test
    fun setupProfilesTest() {

        launchActivity<MainActivity>().use {
            val setupProfilesKey = "prefs.installed.profiles"
            assertTrue(Prefs.getBoolean(setupProfilesKey, true))
            Prefs.updateBoolean(setupProfilesKey, false)

            profile.setupProfiles()

            assertTrue(Prefs.getBoolean(setupProfilesKey, true))

            assertProfilesExists()
        }
    }

    @Test
    fun getProfileListTest() {

        launchActivity<MainActivity>().use {
            val expected = getExpectedProfileList()
            val given = profile.getAvailableProfiles()

            assertTrue("Default profiles does not match", expected == given)
        }
    }

    private fun assertProfilesExists() {
        val expected = getExpectedProfileList()
        val given = profile.getAvailableProfiles()
        Log.e("assertProfilesExists", "Given profiles: $given")
        Log.e("assertProfilesExists", "Expected profiles: $expected")

        assertTrue("Default profiles does not match", expected == given)

        given.forEach {
            profile.loadProfile(it.key)

            assertTrue("Loaded profiles does not match",  profile.getCurrentProfile() == it.key)

            val txt =  if (it.value!!.startsWith("Profile"))  it.value!! else "Profile ${it.value}"

            val vehicleProfile = Espresso.onView(ViewMatchers.withId(R.id.trip_profile))
            vehicleProfile.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            vehicleProfile.check(ViewAssertions.matches(ViewMatchers.isEnabled()))
            vehicleProfile.check(ViewAssertions.matches(ViewMatchers.withText(txt)))
        }
    }

    private fun getExpectedProfileList(): Map<String, String> = mapOf(
            "profile_1" to "Default (BT)",
            "profile_2" to "Alfa 1.75 TBI (BT)",
            "profile_3" to "Alfa 2.0 GME (BT)",
            "profile_4" to "Alfa 2.0 GME (STN,WIFI)",
            "profile_5" to "Alfa 1.75 TBI (STN,WIFI)",
            "profile_6" to "Alfa 2.0 GME (STN,USB)",
            "profile_7" to "Profile 7",
            "profile_8" to "Profile 8",
            "profile_9" to "Profile 9",
            "profile_10" to "Profile 10",
        )
}
