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

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.obd.graphs.R

internal data class SpeedDialViews(
    val connectFab: FloatingActionButton,
    val configureViewFab: FloatingActionButton,
    val configurePidsFab: FloatingActionButton,
    val configureViewLabel: TextView,
    val configurePidsLabel: TextView
)

internal class FabManager(
    private val context: Context,
    private val views: SpeedDialViews,
    private val onConfigureViewClicked: () -> Unit,
    private val onConfigurePidsClicked: () -> Unit
) {
    private var isFabExpanded = false

    private companion object {
        const val ANIM_DURATION = 300L
        const val FAB_ROTATION = 45f

        // Adjust these DP values to control how high the buttons fly
        const val OFFSET_1_DP = -60f
        const val OFFSET_2_DP = -115f
    }

    fun setup() {
        // Initial state setup
        views.configureViewFab.alpha = 0f
        views.configurePidsFab.alpha = 0f

        views.connectFab.setOnLongClickListener {
            if (isFabExpanded) {
                closeSpeedDial()
            } else {
                openSpeedDial()
            }
            true
        }

        views.configureViewFab.setOnClickListener {
            closeSpeedDial()
            onConfigureViewClicked() // Execute the actual action
        }

        views.configurePidsFab.setOnClickListener {
            closeSpeedDial()
            onConfigurePidsClicked() // Execute the actual action
        }
    }

    private fun openSpeedDial() {
        isFabExpanded = true

        // Make views visible before animating
        views.configureViewFab.visibility = View.VISIBLE
        views.configurePidsFab.visibility = View.VISIBLE
        views.configureViewLabel.visibility = View.VISIBLE
        views.configurePidsLabel.visibility = View.VISIBLE

        // Convert Density-Independent Pixels (dp) to actual screen Pixels (px)
        val offset1 = dpToPx(OFFSET_1_DP)
        val offset2 = dpToPx(OFFSET_2_DP)

        // Animate FABs
        views.configureViewFab.animate().translationY(offset1).alpha(1f).setDuration(ANIM_DURATION)
            .start()
        views.configurePidsFab.animate().translationY(offset2).alpha(1f).setDuration(ANIM_DURATION)
            .start()

        // Animate Labels
        views.configureViewLabel.animate().translationY(offset1).alpha(1f)
            .setDuration(ANIM_DURATION).start()
        views.configurePidsLabel.animate().translationY(offset2).alpha(1f)
            .setDuration(ANIM_DURATION).start()

        // Rotate Main FAB
        views.connectFab.animate().rotation(FAB_ROTATION).setDuration(ANIM_DURATION).start()
    }

    private fun closeSpeedDial() {
        isFabExpanded = false

        views.configureViewFab.animate().translationY(0f).alpha(0f).setDuration(ANIM_DURATION)
            .start()
        views.configurePidsFab.animate().translationY(0f).alpha(0f).setDuration(ANIM_DURATION)
            .withEndAction {
                views.configureViewFab.visibility = View.INVISIBLE
                views.configurePidsFab.visibility = View.INVISIBLE
            }.start()

        views.configureViewLabel.animate().translationY(0f).alpha(0f).setDuration(ANIM_DURATION)
            .start()
        views.configurePidsLabel.animate().translationY(0f).alpha(0f).setDuration(ANIM_DURATION)
            .withEndAction {
                views.configureViewLabel.visibility = View.INVISIBLE
                views.configurePidsLabel.visibility = View.INVISIBLE
            }.start()

        views.connectFab.animate().rotation(0f).setDuration(ANIM_DURATION).start()
    }


    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}

internal object FabButtons {
    fun setupSpeedDialView(activity: Activity) {
        val speedDialViews = view(activity)

        val fabManager = FabManager(
            context = activity,
            views = speedDialViews,
            onConfigureViewClicked = {
            },
            onConfigurePidsClicked = {
            }
        )

        fabManager.setup()
    }

    fun view(activity: Activity): SpeedDialViews = SpeedDialViews(
        connectFab = activity.findViewById(R.id.connect_btn),
        configureViewFab = activity.findViewById(R.id.configure_view_btn),
        configurePidsFab = activity.findViewById(R.id.configure_pids_btn),
        configureViewLabel = activity.findViewById(R.id.secondary_action_label),
        configurePidsLabel = activity.findViewById(R.id.third_action_label)
    )
}
