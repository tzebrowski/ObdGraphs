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
package org.obd.graphs.wizard

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import org.obd.graphs.R
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.pid.PidDefinitionDialogFragment
import org.obd.graphs.preferences.updateBoolean

private data class ViewOption(
    val titleResId: Int,
    val enabledPrefKey: String,
    val defaultEnabled: Boolean,
    val pidsKey: String? = null,
    val pidsSource: String? = null
)

private val WIZARD_VIEW_OPTIONS =
    listOf(
        ViewOption(R.string.navigation_title_gauge, "pref.gauge.view.enabled", true, "pref.gauge.displayed_parameter_ids", "gauge"),
        ViewOption(R.string.navigation_title_dashboard, "pref.dash.view.enabled", false, "pref.dash.displayed_parameter_ids", "dashboard"),
        ViewOption(
            R.string.navigation_title_performance,
            "pref.performance.view.enabled",
            false,
            "pref.performance.displayed_parameter_ids",
            "performance"
        ),
        ViewOption(
            R.string.navigation_title_trip_info,
            "pref.trip_info.view.enabled",
            false,
            "pref.trip_info.displayed_parameter_ids",
            "trip_info"
        ),
        ViewOption(R.string.navigation_title_graph, "pref.graph.view.enabled", true, "pref.graph.displayed_parameter_ids", "graph"),
        ViewOption(R.string.navigation_title_giulia, "pref.giulia.view.enabled", true, "pref.giulia.displayed_parameter_ids", "giulia"),
        ViewOption(R.string.navigation_title_drag_racing, "pref.drag_racing.view.enabled", false)
    )

class ViewsStepFragment : Fragment(R.layout.fragment_wizard_views) {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val container = view.findViewById<LinearLayout>(R.id.llWizardViews)

        WIZARD_VIEW_OPTIONS.forEach { option ->
            val row =
                LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

            val checkBox =
                CheckBox(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    text = getString(option.titleResId)
                    isChecked = Prefs.getBoolean(option.enabledPrefKey, option.defaultEnabled)
                    setOnCheckedChangeListener { _, checked -> Prefs.updateBoolean(option.enabledPrefKey, checked) }
                }
            row.addView(checkBox)

            if (option.pidsKey != null && option.pidsSource != null) {
                val selectPidsButton =
                    MaterialButton(requireContext()).apply {
                        text = getString(R.string.wizard_step_views_select_pids)
                        setOnClickListener {
                            PidDefinitionDialogFragment(key = option.pidsKey, source = option.pidsSource)
                                .show(childFragmentManager, null)
                        }
                    }
                row.addView(selectPidsButton)
            }

            container.addView(row)
        }
    }
}
