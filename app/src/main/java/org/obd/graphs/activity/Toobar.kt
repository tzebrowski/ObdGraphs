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
package org.obd.graphs.activity

import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.bottomappbar.BottomAppBar
import org.obd.graphs.R

const val TOOLBAR_TOGGLE_ACTION: String = "toolbar.toggle.event"
const val TOOLBAR_SHOW: String = "toolbar.show.event"
const val TOOLBAR_HIDE: String = "toolbar.hide.event"

private fun toolbarHide(
    bottomAppBar: BottomAppBar,
    views: SpeedDialViews,
    hide: Boolean,
) {
    fun runAnim() {
        val duration = 250L

        val barHeight = bottomAppBar.height.toFloat().takeIf { it > 0 } ?: 500f
        val fabHeight = barHeight + views.connectFab.height.toFloat()

        if (hide) {
            FabButtons.manager?.closeSpeedDial()
        } else {
            bottomAppBar.translationY = barHeight
            bottomAppBar.isVisible = true

            views.connectFab.translationY = fabHeight
            views.connectFab.visibility = View.VISIBLE
        }

        bottomAppBar
            .animate()
            .translationY(if (hide) barHeight else 0f)
            .setDuration(duration)
            .withEndAction { if (hide) bottomAppBar.isVisible = false }
            .start()

        views.connectFab
            .animate()
            .translationY(if (hide) fabHeight else 0f)
            .setDuration(duration)
            .withEndAction {
                if (hide) views.connectFab.visibility = View.GONE
            }.start()

        val subViews =
            listOf(
                views.configureViewFab,
                views.configurePidsFab,
                views.configureViewLabel,
                views.configurePidsLabel,
            )

        // Animate Speed Dial Components along with the main FAB
        subViews.forEach { view ->
            if (!hide && view.visibility == View.GONE) {
                view.visibility = View.INVISIBLE
            }

            view
                .animate()
                .translationY(if (hide) fabHeight else 0f)
                .setDuration(duration)
                .withEndAction {
                    if (hide) view.visibility = View.GONE
                }.start()
        }
    }

    bottomAppBar.post { runAnim() }
}

fun MainActivity.toolbarToggle() =
    toolbar { b, views ->
        toolbarHide(b, views, b.isVisible && views.connectFab.isVisible)
    }

private const val EVENT_THROTTLE_MS = 550L
private var lastEventTime = 0L
private const val TAG = "TB"

fun MainActivity.toolbarHide(hide: Boolean) =
    toolbar { bottomAppBar, views ->
        val currentTime = System.currentTimeMillis()
        val isBarHidden = bottomAppBar.translationY > 0
        if (currentTime - lastEventTime > EVENT_THROTTLE_MS) {
            if ((!isBarHidden && hide) || (isBarHidden && !hide)) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(
                        TAG,
                        "Toolbar.debug: isBarHidden=$isBarHidden request=$hide ts=${currentTime - lastEventTime}",
                    )
                }
                toolbarHide(bottomAppBar, views, hide)
            }
            lastEventTime = currentTime
        }
    }

private fun MainActivity.toolbar(func: (r: BottomAppBar, views: SpeedDialViews) -> Unit) {
    func(findViewById(R.id.bottom_app_bar), FabButtons.view(this))
}
