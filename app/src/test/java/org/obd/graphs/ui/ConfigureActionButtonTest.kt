/*
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
package org.obd.graphs.ui

import android.content.Context
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.obd.graphs.activity.FabButtons
import org.obd.graphs.activity.SpeedDialViews
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.withDataLogger
import org.obd.graphs.bl.query.Query

class ConfigureActionButtonTest {
    private val activity = mockk<FragmentActivity>(relaxed = true)
    private val connectFab = mockk<FloatingActionButton>(relaxed = true)
    private val listener = slot<View.OnClickListener>()

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        mockkStatic(ContextCompat::class)
        every { ContextCompat.getColorStateList(any(), any()) } returns null
        mockkStatic("org.obd.graphs.bl.datalogger.DataLoggerConnectorKt")
        every { withDataLogger(any(), any()) } just Runs
        mockkObject(DataLoggerRepository)
        every { DataLoggerRepository.isRunning() } returns false
        mockkObject(FabButtons)
        every { FabButtons.view(activity) } returns
            SpeedDialViews(connectFab, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
        every { connectFab.setOnClickListener(capture(listener)) } just Runs
        every { connectFab.context } returns activity
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `click after the fragment is detached does not crash`() {
        val fragment = mockk<Fragment>(relaxed = true)
        every { fragment.activity } returns activity
        fragment.configureActionButton(mockk<Query>())

        // Navigated away: the button keeps the listener, the fragment has no context any more.
        every { fragment.requireContext() } throws IllegalStateException("Fragment not attached to a context.")
        listener.captured.onClick(connectFab)

        verify { withDataLogger(activity as Context, any()) }
    }
}
