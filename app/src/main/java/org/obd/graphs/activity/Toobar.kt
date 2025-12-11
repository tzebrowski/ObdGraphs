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
package org.obd.graphs.activity

import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.obd.graphs.R

const val TOOLBAR_TOGGLE_ACTION: String = "toolbar.toggle.event"
const val TOOLBAR_SHOW: String = "toolbar.reset.animation"

fun toolbarHide(
    bottomNavigationView: BottomNavigationView,
    bottomAppBar: BottomAppBar,
    floatingActionButton: FloatingActionButton,
    hide: Boolean,
) {

    // Define the action to run after layout is confirmed
    fun runAnim() {
        val duration = 200L

        // Calculate distinct heights
        val navHeight = bottomNavigationView.height.toFloat()
        val barHeight = bottomAppBar.height.toFloat()

        // Handle Visibility to prevent "Ghost Clicks"
        // If showing, make visible IMMEDIATELY before animating in
        if (!hide) {
            bottomNavigationView.isVisible = true
            bottomAppBar.isVisible = true
            floatingActionButton.show() // FAB has its own built-in visibility method
        }

        // Animate BottomNavigation
        val navTargetY = if (hide) navHeight else 0f
        bottomNavigationView.clearAnimation()
        bottomNavigationView.animate()
            .translationY(navTargetY)
            .setDuration(duration)
            .withEndAction {
                // If hiding, set GONE only after animation finishes
                if (hide) bottomNavigationView.isVisible = false
            }
            .start()

        // Animate BottomAppBar
        val barTargetY = if (hide) barHeight else 0f
        bottomAppBar.clearAnimation()
        bottomAppBar.animate()
            .translationY(barTargetY)
            .setDuration(duration)
            .withEndAction {
                if (hide) bottomAppBar.isVisible = false
            }
            .start()

        // Ideally, use the FAB's built-in show/hide for a scale animation which is cleaner
        if (hide) {
            floatingActionButton.hide()
        } else {
            floatingActionButton.show()
        }
    }

    // Fix "Zero Height" bug: Wait for layout if height is 0
    if (bottomNavigationView.height == 0 && hide) {
        bottomNavigationView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    bottomNavigationView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    runAnim()
                }
            }
        )
    } else {
        runAnim()
    }
}

fun MainActivity.toolbarToggle() =
    toolbar { a, b, c ->
        toolbarHide(a, b, c, a.translationY == 0f && b.isVisible && c.isVisible)
    }

fun MainActivity.toolbarHide(hide: Boolean) =
    toolbar { bottomNavigationView, bottomAppBar, floatingActionButton ->
        toolbarHide(bottomNavigationView, bottomAppBar, floatingActionButton, hide)
    }

private fun MainActivity.toolbar(func: (p: BottomNavigationView, r: BottomAppBar, c: FloatingActionButton) -> Unit) {
    func(findViewById(R.id.bottom_nav_view), findViewById(R.id.bottom_app_bar), findViewById(R.id.connect_btn))
}