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
package org.obd.graphs.preferences

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import androidx.preference.Preference
import org.obd.graphs.LanguageManager
import org.obd.graphs.bl.datalogger.DataLoggerRepository

internal class LanguagePreferences(
    context: Context,
    attrs: AttributeSet?,
) : Preference(context, attrs) {

    init {
        updateSummary()
    }

    override fun onClick() {
        super.onClick()
        val activity = getActivity() ?: return

        LanguageManager.showLanguageSelectionDialog(activity) { localeTag ->
            DataLoggerRepository.updateTranslations(localeTag)
            updateSummary()
            activity.recreate()
        }
    }

    private fun updateSummary() {
        val currentLangCode = LanguageManager.getStoredLanguage(context)
        if (currentLangCode.isEmpty()) return

        val codes = context.resources.getStringArray(org.obd.graphs.commons.R.array.language_codes)
        val names = context.resources.getStringArray(org.obd.graphs.commons.R.array.language_names)

        val index = codes.indexOf(currentLangCode)
        summary =
            if (index >= 0) {
                names[index]
            } else {
                currentLangCode
            }
    }

    private fun getActivity(): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }
}
