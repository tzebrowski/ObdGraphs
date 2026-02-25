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

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import androidx.preference.SwitchPreferenceCompat
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.colorize

class BroadcastEventCheckBoxPreference(
    context: Context,
    private val attrs: AttributeSet?,
) : SwitchPreferenceCompat(context, attrs) {
    private val experimental = getAttribute("experimental").toBooleanStrictOrNull() ?: false
    private val broadcastEvent = getAttribute("broadcastEvent")

    override fun setChecked(checked: Boolean) {
        val changed = isChecked != checked
        super.setChecked(checked)

        if (changed) {
            if (broadcastEvent != null && broadcastEvent.isNotEmpty()) {
                Log.d("BroadcastEventCheckBoxPreference", "Visibility changed to: $checked for $broadcastEvent")
                sendBroadcastEvent(broadcastEvent)
            }
        }
    }

    override fun getSummary(): CharSequence? =
        if (experimental) {
            super.getSummary().toString().colorize(COLOR_CARDINAL, Typeface.NORMAL, 0, 33, 1.0f)
        } else {
            super.getSummary()
        }

    private fun getAttribute(attrName: String): String =
        if (attrs == null) {
            ""
        } else {
            val givenValue: String? =
                (0 until attrs.attributeCount)
                    .filter { index -> attrs.getAttributeName(index) == attrName }
                    .map { index -> attrs.getAttributeValue(index) }
                    .firstOrNull()
            givenValue ?: ""
        }
}
