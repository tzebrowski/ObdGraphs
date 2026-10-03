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
package org.obd.graphs.preferences

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN

/** One entry in an adapter picker: what gets persisted, and what the user sees. */
internal class Device(
    val address: String,
    val label: Spanned
)

/** Renders "name (MAC)" with the MAC de-emphasised. Shared by the Classic and BLE pickers. */
internal fun formatDeviceLabel(
    name: String?,
    address: String
): Spanned {
    val text = "${name ?: address} ($address)"
    val spanned = SpannableString(text)

    return try {
        spanned.apply {
            val endIndexOf = text.indexOf(")") + 1
            val startIndexOf = text.indexOf("(")
            setSpan(
                RelativeSizeSpan(0.5f),
                startIndexOf,
                endIndexOf,
                0
            )

            setSpan(
                ForegroundColorSpan(COLOR_PHILIPPINE_GREEN),
                startIndexOf,
                endIndexOf,
                0
            )
        }
    } catch (e: Throwable) {
        spanned
    }
}
