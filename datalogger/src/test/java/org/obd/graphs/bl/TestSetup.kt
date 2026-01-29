 /**
 * Copyright 2019-2026, Tomasz Żebrowski
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
package org.obd.graphs.bl

import android.content.ContextWrapper
import android.content.SharedPreferences
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.obd.graphs.Permissions
import org.obd.graphs.getContext
import org.obd.graphs.preferences.Prefs

open class TestSetup {
    protected val sharedPrefs = mockk<SharedPreferences>(relaxed = true)
    protected val editor = mockk<SharedPreferences.Editor>(relaxed = true)

    protected lateinit var context: ContextWrapper

    protected fun mockContext() {
        context = ApplicationProvider.getApplicationContext()
        mockkStatic(::getContext)
        every { getContext() } returns context
    }

    protected fun mockLog() {
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.isLoggable(any(), any()) } returns false
    }

    protected fun mockPrefs() {
        mockkStatic("org.obd.graphs.preferences.PreferencesKt")
        every { Prefs } returns sharedPrefs
        every { sharedPrefs.edit() } returns editor
        every { sharedPrefs.all } returns emptyMap()
        every { sharedPrefs.getString(any(), any()) } answers { secondArg() }
        every { sharedPrefs.getBoolean(any(), any()) } returns false // Default to false for installation check
        every { sharedPrefs.registerOnSharedPreferenceChangeListener(any()) } just Runs
    }

    open fun setup() {
        MockKAnnotations.init(this)
        mockContext()
        mockPrefs()
        mockLog()

        mockkObject(Permissions)
        every { Permissions.hasNotificationPermissions(any()) } returns true
        every { Permissions.hasLocationPermissions(any()) } returns true
    }
}
