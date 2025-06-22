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
package org.obd.graphs.preferences.pid

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.preference.DialogPreference
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.colorize

class PidDefinitionListPreferences(
    context: Context,
    private val attrs: AttributeSet?,
) : DialogPreference(context, attrs) {
    val source = getAttribute("source")

    override fun isEnabled(): Boolean =
        !(dataLoggerSettings.instance().adapter.individualQueryStrategyEnabled && (source == "high" || source == "low"))

    override fun getSummary(): CharSequence? =
        if (isEnabled) {
            super.getSummary()
        } else {
            context
                .getString(R.string.pref_adapter_query_view_individual_dialog_disabled_warning)
                .colorize(COLOR_CARDINAL, Typeface.NORMAL, 1.0f)
        }

    private fun getAttribute(attrName: String): String =
        if (attrs == null) {
            ""
        } else {
            val priority: String? =
                (0 until attrs.attributeCount)
                    .filter { index -> attrs.getAttributeName(index) == attrName }
                    .map { index -> attrs.getAttributeValue(index) }
                    .firstOrNull()
            priority ?: ""
        }
}
