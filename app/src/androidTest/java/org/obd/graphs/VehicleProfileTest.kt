package org.obd.graphs

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Matchers.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.activity.MainActivity

@RunWith(AndroidJUnit4::class)
class VehicleProfileTest {

    @Test
    fun loadVehicleProfileTest() {

        launchActivity<MainActivity>().use {
             mapOf("profile_2" to "Profile Alfa 1.75 TBI", "profile_1" to "Profile Default", "profile_3" to "Profile Alfa 2.0 GME").forEach {
                 org.obd.graphs.preferences.profile.vehicleProfile.loadProfile(it.key)

                 val vehicleProfile = Espresso.onView(ViewMatchers.withId(R.id.vehicle_profile))
                 vehicleProfile.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                 vehicleProfile.check(ViewAssertions.matches(ViewMatchers.isEnabled()))
                 vehicleProfile.check(ViewAssertions.matches(ViewMatchers.withText(it.value)))
             }
        }
    }
}
