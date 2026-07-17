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
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import org.obd.graphs.R
import org.obd.graphs.theme.THEME_DARK
import org.obd.graphs.theme.THEME_LIGHT
import org.obd.graphs.theme.THEME_SYSTEM
import org.obd.graphs.theme.ThemeManager

class ThemeStepFragment : Fragment(R.layout.fragment_wizard_theme) {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val radioGroup = view.findViewById<RadioGroup>(R.id.rgWizardTheme)
        val rbSystem = view.findViewById<RadioButton>(R.id.rbWizardThemeSystem)
        val rbLight = view.findViewById<RadioButton>(R.id.rbWizardThemeLight)
        val rbDark = view.findViewById<RadioButton>(R.id.rbWizardThemeDark)

        when (ThemeManager.getStoredTheme(requireContext())) {
            THEME_LIGHT -> rbLight.isChecked = true
            THEME_DARK -> rbDark.isChecked = true
            else -> rbSystem.isChecked = true
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val theme =
                when (checkedId) {
                    R.id.rbWizardThemeLight -> THEME_LIGHT
                    R.id.rbWizardThemeDark -> THEME_DARK
                    else -> THEME_SYSTEM
                }
            ThemeManager.saveTheme(requireContext(), theme)
        }
    }
}
