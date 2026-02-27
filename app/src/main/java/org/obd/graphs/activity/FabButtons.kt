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

import android.view.View
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.obd.graphs.R

object FabButtons {
    private var isFabExpanded = false

    fun setup(mainActivity: MainActivity) {

        val connectFab = mainActivity.findViewById<FloatingActionButton>(R.id.connect_btn)

        val configureViewFab =
            mainActivity.findViewById<FloatingActionButton>(R.id.configure_view_btn)

        val configurePIDsFab =
            mainActivity.findViewById<FloatingActionButton>(R.id.configure_pids_btn)

        val secondaryLabel = mainActivity.findViewById<TextView>(R.id.secondary_action_label)
        val thirdLabel = mainActivity.findViewById<TextView>(R.id.third_action_label)

        configureViewFab.alpha = 0f
        configurePIDsFab.alpha = 0f

        connectFab.setOnLongClickListener {
            if (isFabExpanded) {
                closeSpeedDial(
                    connectFab,
                    configureViewFab,
                    configurePIDsFab,
                    secondaryLabel,
                    thirdLabel
                )
            } else {
                openSpeedDial(
                    connectFab,
                    configureViewFab,
                    configurePIDsFab,
                    secondaryLabel,
                    thirdLabel
                )
            }
            true
        }

        configureViewFab.setOnClickListener {
            closeSpeedDial(
                connectFab,
                configureViewFab,
                configurePIDsFab,
                secondaryLabel, thirdLabel
            )
        }
        configurePIDsFab.setOnClickListener {
            closeSpeedDial(
                connectFab,
                configureViewFab,
                configurePIDsFab,
                secondaryLabel,
                thirdLabel
            )
        }
    }

    private fun openSpeedDial(
        mainFab: FloatingActionButton,
        secondaryFab: FloatingActionButton,
        thirdFab: FloatingActionButton,
        secondaryLabel: TextView,
        thirdLabel: TextView
    ) {
        isFabExpanded = true

        secondaryFab.visibility = View.VISIBLE
        thirdFab.visibility = View.VISIBLE
        secondaryLabel.visibility = View.VISIBLE
        thirdLabel.visibility = View.VISIBLE

        secondaryFab.animate().translationY(-180f).alpha(1f).setDuration(300).start()
        thirdFab.animate().translationY(-320f).alpha(1f).setDuration(300).start()

        secondaryLabel.animate().translationY(-180f).alpha(1f).setDuration(300).start()
        thirdLabel.animate().translationY(-320f).alpha(1f).setDuration(300).start()
        mainFab.animate().rotation(45f).setDuration(300).start()
    }

    private fun closeSpeedDial(
        mainFab: FloatingActionButton,
        secondaryFab: FloatingActionButton,
        thirdFab: FloatingActionButton,
        secondaryLabel: TextView,
        thirdLabel: TextView

    ) {
        isFabExpanded = false

        secondaryFab
            .animate()
            .translationY(0f)
            .alpha(0f)
            .setDuration(300)
            .start()
        thirdFab
            .animate()
            .translationY(0f)
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                secondaryFab.visibility = View.INVISIBLE
                thirdFab.visibility = View.INVISIBLE
            }.start()
        mainFab
            .animate()
            .rotation(0f)
            .setDuration(300)
            .start()

        secondaryLabel.animate().translationY(0f).alpha(0f).setDuration(300).start()
        thirdLabel.animate().translationY(0f).alpha(0f).setDuration(300).withEndAction {
            secondaryLabel.visibility = View.INVISIBLE
            thirdLabel.visibility = View.INVISIBLE
        }.start()


    }
}
