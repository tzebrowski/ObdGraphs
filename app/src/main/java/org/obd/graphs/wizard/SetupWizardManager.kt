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

import android.content.Context

private const val PREFS_FILE = "setup_wizard_prefs"
private const val KEY_COMPLETED = "setup_wizard_completed"

object SetupWizardManager {
    fun shouldShowWizard(context: Context): Boolean {
        if (isCompleted(context)) return false
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return info.firstInstallTime == info.lastUpdateTime
    }

    fun isCompleted(context: Context): Boolean = prefs(context).getBoolean(KEY_COMPLETED, false)

    fun markCompleted(context: Context) {
        prefs(context).edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
}
