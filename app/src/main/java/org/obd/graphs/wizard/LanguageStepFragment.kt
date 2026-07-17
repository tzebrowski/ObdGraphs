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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.obd.graphs.R
import org.obd.graphs.language.LanguageAdapter
import org.obd.graphs.language.LanguageManager

class LanguageStepFragment : Fragment(R.layout.fragment_wizard_language) {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val names = resources.getStringArray(org.obd.graphs.commons.R.array.language_names)
        val codes = resources.getStringArray(org.obd.graphs.commons.R.array.language_codes)

        val currentLangCode = LanguageManager.getStoredLanguage(requireContext())
        val selectedIndex = codes.indexOf(currentLangCode).takeIf { it >= 0 } ?: -1

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvWizardLanguages)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter =
            LanguageAdapter(names, selectedIndex) { index ->
                LanguageManager.saveLanguage(requireContext(), codes[index])
                requireActivity().recreate()
            }
    }
}
