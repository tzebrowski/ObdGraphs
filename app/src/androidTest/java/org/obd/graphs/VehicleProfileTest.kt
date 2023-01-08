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
    fun resetProfileTest() {

        launchActivity<MainActivity>().use {

            // lets use this profiles as default
            vehicleProfile.loadProfile("profile_5")

            val propName = "pref.adapter.batch.enabled";
            assertEquals(Prefs.getBoolean(propName, true), true)

            // changing the value of property under profile_5
            Prefs.updateBoolean("pref.adapter.batch.enabled", false)
            assertEquals(Prefs.getBoolean(propName, false), false)

            // resetting profiles
            vehicleProfile.reset()
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

            vehicleProfile.setupProfiles()

            assertTrue(Prefs.getBoolean(setupProfilesKey, true))

            assertProfilesExists()
        }
    }

    @Test
    fun getProfileListTest() {

        launchActivity<MainActivity>().use {
            val expected = getExpectedProfileList()
            val given = getGivenProfileList()

            assertTrue("Default profiles does not match", expected == given)
        }
    }

    private fun assertProfilesExists() {
        val expected = getExpectedProfileList()
        val given = getGivenProfileList()

        assertTrue("Default profiles does not match", expected == given)

        given.forEach {
            vehicleProfile.loadProfile(it.key)

            assertTrue("Loaded profiles does not match",  vehicleProfile.getCurrentProfile() == it.key)

            val txt =  if (it.value!!.startsWith("Profile"))  it.value!! else "Profile ${it.value}"

            val vehicleProfile = Espresso.onView(ViewMatchers.withId(R.id.vehicle_profile))
            vehicleProfile.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            vehicleProfile.check(ViewAssertions.matches(ViewMatchers.isEnabled()))
            vehicleProfile.check(ViewAssertions.matches(ViewMatchers.withText(txt)))
        }
    }

    private fun getGivenProfileList() =  vehicleProfile.getProfileList()

    private fun getExpectedProfileList(): Map<String, String> = mapOf(
            "profile_1" to "Default",
            "profile_2" to "Alfa 1.75 TBI",
            "profile_3" to "Alfa 2.0 GME",
            "profile_4" to "Alfa 2.0 GME (STNxx)",
            "profile_5" to "5"
        )
}
