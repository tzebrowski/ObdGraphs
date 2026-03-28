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

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.preference.ListPreference
import org.obd.graphs.BuildConfig

private const val TAG = "ConnectionType"
private const val MOCK_CONNECTION_TYPE = "mock"

class ConnectionTypeListPreference(
    context: Context,
    attrs: AttributeSet?
) : ListPreference(context, attrs) {
    init {
        if (BuildConfig.DEBUG) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Keeping mock connection type")
            }
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Filtering out mock connection type")
            }

            entries = entries.filter { p -> p != MOCK_CONNECTION_TYPE }.toTypedArray()
            entryValues = entryValues.filter { p -> p != MOCK_CONNECTION_TYPE }.toTypedArray()
        }
    }
}
