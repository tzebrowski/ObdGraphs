package org.obd.graphs

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
import org.obd.graphs.preferences.profile.vehicleProfile
import org.obd.graphs.preferences.updateBoolean

@RunWith(AndroidJUnit4ClassRunner::class)
class VehicleProfileTest {

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
            vehicleProfile.setupProfiles()

            assertTrue(Prefs.getBoolean(setupProfilesKey, true))

            assertProfilesExists()
        }
    }

    private fun assertProfilesExists() {
        mapOf(
            "profile_2" to "Profile Alfa 1.75 TBI",
            "profile_1" to "Profile Default",
            "profile_3" to "Profile Alfa 2.0 GME"
        ).forEach {
            vehicleProfile.loadProfile(it.key)

            val vehicleProfile = Espresso.onView(ViewMatchers.withId(R.id.vehicle_profile))
            vehicleProfile.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            vehicleProfile.check(ViewAssertions.matches(ViewMatchers.isEnabled()))
            vehicleProfile.check(ViewAssertions.matches(ViewMatchers.withText(it.value)))
        }
    }
}
