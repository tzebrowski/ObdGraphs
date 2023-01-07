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

@RunWith(AndroidJUnit4ClassRunner::class)
class MainActivityTest {

    @Test
    fun connectButtonTest() {

        launchActivity<MainActivity>().use {
            val connectButton = Espresso.onView(ViewMatchers.withId(R.id.connect_btn))
            connectButton.check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            connectButton.check(ViewAssertions.matches(ViewMatchers.isEnabled()))
            connectButton.check(ViewAssertions.matches(ViewMatchers.withContentDescription(R.string.main_activity_btn_start)))
        }
    }
}
