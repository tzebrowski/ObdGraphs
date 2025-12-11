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
package org.obd.graphs.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.obd.graphs.R
import org.obd.graphs.activity.MainActivity
import org.obd.graphs.integrations.gcp.gdrive.DriveBackupManager
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getString
import org.obd.graphs.profile.profile

internal class BackupManager(activity: MainActivity) {

    private val driveBackupManager: DriveBackupManager =
        DriveBackupManager.instance(activity.getString(R.string.ANDROID_WEB_CLIENT_ID), activity)

    suspend fun backup() =
        withContext(Dispatchers.Main) {
            if (isCloudBackup()) {
                profile.exportBackup()?.let { file ->
                    driveBackupManager.exportBackup(file)
                }
            } else {
                withContext(Dispatchers.IO) {
                    profile.exportBackup()
                }
            }
        }

    suspend fun restore() =
        withContext(Dispatchers.Main) {
            if (isCloudBackup()) {
                driveBackupManager.restoreBackup { file ->
                    profile.restoreBackup(file)
                }
            } else {
                withContext(Dispatchers.IO) {
                    profile.restoreBackup()
                }
            }
        }

    private fun isCloudBackup() = Prefs.getString("pref.backup.type") == "Cloud"
}
