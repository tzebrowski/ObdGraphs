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
package org.obd.graphs.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.obd.graphs.CUSTOM_PIDS_PUBLISH_NO_FILE
import org.obd.graphs.MODULES_LIST_CHANGED_EVENT
import org.obd.graphs.R
import org.obd.graphs.USER_CUSTOM_PIDS_FILE
import org.obd.graphs.activity.MainActivity
import org.obd.graphs.integrations.gcp.gdrive.CustomPidsDriveManager
import org.obd.graphs.sendBroadcastEvent
import java.io.File

internal class CustomPidsBackupManager(
    private val activity: MainActivity
) {
    private val driveManager: CustomPidsDriveManager =
        CustomPidsDriveManager.instance(activity.getString(R.string.ANDROID_WEB_CLIENT_ID), activity)

    private fun localFile(): File = File(activity.filesDir, USER_CUSTOM_PIDS_FILE)

    suspend fun publish() =
        withContext(Dispatchers.Main) {
            val file = localFile()
            if (file.exists()) {
                driveManager.exportCustomPids(file)
            } else {
                sendBroadcastEvent(CUSTOM_PIDS_PUBLISH_NO_FILE)
            }
        }

    suspend fun download() =
        withContext(Dispatchers.Main) {
            driveManager.importCustomPids { downloaded ->
                downloaded.copyTo(localFile(), overwrite = true)
                downloaded.delete()
                sendBroadcastEvent(MODULES_LIST_CHANGED_EVENT)
            }
        }
}
