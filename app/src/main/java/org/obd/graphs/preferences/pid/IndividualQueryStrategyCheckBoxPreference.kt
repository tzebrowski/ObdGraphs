/**
 * Copyright 2019-2024, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.obd.graphs.preferences.pid

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import org.obd.graphs.R
import org.obd.graphs.activity.navigateToPreferencesScreen
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.colorize

class IndividualQueryStrategyCheckBoxPreference(
    context: Context,
    attrs: AttributeSet?
) :
    CheckBoxPreference(context, attrs) {
    init {
        onPreferenceChangeListener = OnPreferenceChangeListener { _, _ ->
            navigateToPreferencesScreen("pref.registry")
            true
        }
    }

    override fun getSummary(): CharSequence =
        context.getString(R.string.pref_adapter_query_view_individual_enabled_summary)
            .colorize(COLOR_CARDINAL, Typeface.NORMAL,0,33, 1.0f)

}