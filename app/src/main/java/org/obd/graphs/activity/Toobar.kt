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

import android.view.View
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
    fun runAnim() {
        val duration = 250L

        val navHeight = bottomNavigationView.height.toFloat().takeIf { it > 0 } ?: 500f
        val barHeight = bottomAppBar.height.toFloat().takeIf { it > 0 } ?: 500f
        val fabHeight = barHeight + floatingActionButton.height.toFloat()

        if (!hide) {
            bottomNavigationView.translationY = navHeight
            bottomAppBar.translationY = barHeight
            floatingActionButton.translationY = fabHeight

            bottomNavigationView.isVisible = true
            bottomAppBar.isVisible = true
            floatingActionButton.visibility = View.VISIBLE
        }

        bottomNavigationView
            .animate()
            .translationY(if (hide) navHeight else 0f)
            .setDuration(duration)
            .withEndAction { if (hide) bottomNavigationView.isVisible = false }
            .start()

        bottomAppBar
            .animate()
            .translationY(if (hide) barHeight else 0f)
            .setDuration(duration)
            .withEndAction { if (hide) bottomAppBar.isVisible = false }
            .start()

        floatingActionButton
            .animate()
            .translationY(if (hide) fabHeight else 0f)
            .setDuration(duration)
            .withEndAction {
                if (hide) floatingActionButton.visibility = View.GONE
            }.start()
    }

    bottomNavigationView.post { runAnim() }
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
