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
