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
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.obd.graphs.R

internal data class SpeedDialViews(
    val connectFab: FloatingActionButton,
    val configureViewFab: FloatingActionButton,
    val configurePidsFab: FloatingActionButton,
    val configureViewLabel: TextView,
    val configurePidsLabel: TextView,
)

internal class FabManager(
    private val context: Context,
    private val views: SpeedDialViews,
    private val onConfigureViewClicked: () -> Unit,
    private val onConfigurePidsClicked: () -> Unit,
) {
    private var isFabExpanded = false

    val isMainFabVisible: Boolean
        get() = views.connectFab.isVisible

    private companion object {
        const val ANIM_DURATION = 300L
        const val FAB_ROTATION = 45f

        const val OFFSET_1_DP = -60f
        const val OFFSET_2_DP = -115f
    }

    fun setup() {
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
            onConfigureViewClicked()
        }

        views.configurePidsFab.setOnClickListener {
            closeSpeedDial()
            onConfigurePidsClicked()
        }
    }

    fun animateHideShow(
        hide: Boolean,
        barHeight: Float,
        duration: Long,
    ) {
        val fabHeight = barHeight + views.connectFab.height.toFloat()

        if (hide) {
            closeSpeedDial()
        } else {
            views.connectFab.translationY = fabHeight
            views.connectFab.visibility = View.VISIBLE
        }

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

        subViews.forEach { view ->
            if (!hide && view.isGone) {
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

    private fun openSpeedDial() {
        isFabExpanded = true

        views.configureViewFab.visibility = View.VISIBLE
        views.configurePidsFab.visibility = View.VISIBLE
        views.configureViewLabel.visibility = View.VISIBLE
        views.configurePidsLabel.visibility = View.VISIBLE

        val offset1 = dpToPx(OFFSET_1_DP)
        val offset2 = dpToPx(OFFSET_2_DP)

        views.configureViewFab
            .animate()
            .translationY(offset1)
            .alpha(1f)
            .setDuration(ANIM_DURATION)
            .start()
        views.configurePidsFab
            .animate()
            .translationY(offset2)
            .alpha(1f)
            .setDuration(ANIM_DURATION)
            .start()

        views.configureViewLabel
            .animate()
            .translationY(offset1)
            .alpha(1f)
            .setDuration(ANIM_DURATION)
            .start()
        views.configurePidsLabel
            .animate()
            .translationY(offset2)
            .alpha(1f)
            .setDuration(ANIM_DURATION)
            .start()

        views.connectFab
            .animate()
            .rotation(FAB_ROTATION)
            .setDuration(ANIM_DURATION)
            .start()
    }

    fun closeSpeedDial() {
        if (!isFabExpanded) return
        isFabExpanded = false

        views.configureViewFab
            .animate()
            .translationY(0f)
            .alpha(0f)
            .setDuration(ANIM_DURATION)
            .start()

        views.configurePidsFab
            .animate()
            .translationY(0f)
            .alpha(0f)
            .setDuration(ANIM_DURATION)
            .withEndAction {
                views.configureViewFab.visibility = View.INVISIBLE
                views.configurePidsFab.visibility = View.INVISIBLE
            }.start()

        views.configureViewLabel
            .animate()
            .translationY(0f)
            .alpha(0f)
            .setDuration(ANIM_DURATION)
            .start()

        views.configurePidsLabel
            .animate()
            .translationY(0f)
            .alpha(0f)
            .setDuration(ANIM_DURATION)
            .withEndAction {
                views.configureViewLabel.visibility = View.INVISIBLE
                views.configurePidsLabel.visibility = View.INVISIBLE
            }.start()

        views.connectFab
            .animate()
            .rotation(0f)
            .setDuration(ANIM_DURATION)
            .start()
    }

    private fun dpToPx(dp: Float): Float = dp * context.resources.displayMetrics.density
}

internal object FabButtons {
    var manager: FabManager? = null

    fun setupSpeedDialView(activity: Activity) {
        val speedDialViews = view(activity)

        manager =
            FabManager(
                context = activity,
                views = speedDialViews,
                onConfigureViewClicked = {
                    NavigationRouter.navigateToPreferences(activity)
                },
                onConfigurePidsClicked = {
                    NavigationRouter.navigateToPIDsPreferences(activity)
                },
            )

        manager?.setup()
    }

    fun view(activity: Activity): SpeedDialViews =
        SpeedDialViews(
            connectFab = activity.findViewById(R.id.connect_btn),
            configureViewFab = activity.findViewById(R.id.configure_view_btn),
            configurePidsFab = activity.findViewById(R.id.configure_pids_btn),
            configureViewLabel = activity.findViewById(R.id.configure_view_action_label),
            configurePidsLabel = activity.findViewById(R.id.configure_pids_action_label),
        )
}
