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
package org.obd.graphs.integrations.gcp.gdrive

import android.app.Activity
import android.util.Log
import org.obd.graphs.CUSTOM_PIDS_DOWNLOAD_FAILED
import org.obd.graphs.CUSTOM_PIDS_DOWNLOAD_NO_FILES
import org.obd.graphs.CUSTOM_PIDS_DOWNLOAD_SUCCESSFUL
import org.obd.graphs.CUSTOM_PIDS_PUBLISH_FAILED
import org.obd.graphs.CUSTOM_PIDS_PUBLISH_SUCCESSFUL
import org.obd.graphs.SCREEN_UNLOCK_PROGRESS_EVENT
import org.obd.graphs.USER_CUSTOM_PIDS_FILE
import org.obd.graphs.integrations.gcp.gdrive.DriveHelper.findFolderIdRecursive
import org.obd.graphs.integrations.gcp.gdrive.DriveHelper.uploadFile
import org.obd.graphs.sendBroadcastEvent
import java.io.File
import java.io.FileOutputStream

private const val CUSTOM_PIDS_FOLDER = "mygiulia"
private const val TAG = "CustomPidsDrive"

internal class DefaultCustomPidsDriveManager(
    webClientId: String,
    activity: Activity
) : AbstractDriveManager(webClientId, activity, null),
    CustomPidsDriveManager {
    override suspend fun exportCustomPids(file: File) =
        signInAndExecute("exportCustomPids") { token ->
            executeDriveOperation(
                accessToken = token,
                onFailure = { sendBroadcastEvent(CUSTOM_PIDS_PUBLISH_FAILED) },
                onFinally = { sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT) }
            ) { drive ->
                val folderId = drive.findFolderIdRecursive(CUSTOM_PIDS_FOLDER)
                drive.uploadFile(file, USER_CUSTOM_PIDS_FILE, folderId, mimeType = "application/json")
                sendBroadcastEvent(CUSTOM_PIDS_PUBLISH_SUCCESSFUL)
            }
        }

    override suspend fun importCustomPids(onImport: (File) -> Unit) =
        signInAndExecute("importCustomPids") { token ->
            executeDriveOperation(
                accessToken = token,
                onFailure = { sendBroadcastEvent(CUSTOM_PIDS_DOWNLOAD_FAILED) },
                onFinally = { sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT) }
            ) { drive ->
                val fileList =
                    drive
                        .files()
                        .list()
                        .setSpaces("drive")
                        .setQ("name = '$USER_CUSTOM_PIDS_FILE' and trashed = false")
                        .setOrderBy("createdTime desc")
                        .setFields("files(id, createdTime)")
                        .execute()

                val remoteFile = fileList.files.firstOrNull()

                if (remoteFile != null) {
                    Log.d(TAG, "Found custom PIDs file: ${remoteFile.id}")
                    val target = File(activity.filesDir, "downloaded_$USER_CUSTOM_PIDS_FILE")

                    FileOutputStream(target).use { output ->
                        drive.files().get(remoteFile.id).executeMediaAndDownloadTo(output)
                    }

                    onImport(target)
                    sendBroadcastEvent(CUSTOM_PIDS_DOWNLOAD_SUCCESSFUL)
                } else {
                    Log.d(TAG, "No custom PIDs file found.")
                    sendBroadcastEvent(CUSTOM_PIDS_DOWNLOAD_NO_FILES)
                }
            }
        }
}
