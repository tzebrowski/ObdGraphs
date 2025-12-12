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
package org.obd.graphs.integrations.gcp.gdrive

import android.app.Activity
import android.util.Log
import org.obd.graphs.*
import java.io.File
import java.io.FileOutputStream

private const val BACKUP_FILE_NAME = "mygiulia_config_backup.properties"
private const val BACKUP_FOLDER = "mygiulia"
private const val TAG = "DriveBackup"

internal open class DefaultDriveBackupManager(
    webClientId: String,
    activity: Activity,
) : AbstractDriveManager(webClientId, activity, null), DriveBackupManager {

    override suspend fun exportBackup(file: File) =
        signInAndExecute("exportBackup") { token ->
            executeDriveOperation(
                accessToken = token,
                onFailure = { sendBroadcastEvent(BACKUP_FAILED) },
                onFinally = { sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT) }
            ) { drive ->
                val folderId = drive.findFolderIdRecursive(BACKUP_FOLDER)
                drive.uploadFile(file, folderId)
                sendBroadcastEvent(BACKUP_SUCCESSFUL)
            }
        }

    override suspend fun restoreBackup(onRestore: (File) -> Unit) =
        signInAndExecute("restoreBackup") { token ->
            executeDriveOperation(
                accessToken = token,
                onFailure = { sendBroadcastEvent(BACKUP_RESTORE_FAILED) },
                onFinally = { sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT) }
            ) { drive ->
                val fileList = drive.files().list()
                    .setSpaces("drive")
                    .setQ("name = '$BACKUP_FILE_NAME' and trashed = false")
                    .setOrderBy("createdTime desc")
                    .setFields("files(id, createdTime)")
                    .execute()

                val remoteFile = fileList.files.firstOrNull()

                if (remoteFile != null) {
                    Log.d(TAG, "Found backup file: ${remoteFile.id}")
                    val target = File(activity.filesDir, "restored_backup.json")

                    FileOutputStream(target).use { output ->
                        drive.files().get(remoteFile.id).executeMediaAndDownloadTo(output)
                    }

                    onRestore(target)
                    sendBroadcastEvent(BACKUP_RESTORE_SUCCESSFUL)
                } else {
                    Log.d(TAG, "No backup file found.")
                    sendBroadcastEvent(BACKUP_RESTORE_NO_FILES)
                }
            }
        }
}
