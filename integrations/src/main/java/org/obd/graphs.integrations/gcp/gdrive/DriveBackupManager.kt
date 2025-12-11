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
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.obd.graphs.BACKUP_FAILED
import org.obd.graphs.BACKUP_RESTORE_FAILED
import org.obd.graphs.BACKUP_RESTORE_NO_FILES
import org.obd.graphs.BACKUP_RESTORE_SUCCESSFUL
import org.obd.graphs.BACKUP_SUCCESSFUL
import org.obd.graphs.SCREEN_UNLOCK_PROGRESS_EVENT
import org.obd.graphs.integrations.gcp.authorization.Action
import org.obd.graphs.integrations.gcp.authorization.AuthorizationManager
import org.obd.graphs.sendBroadcastEvent
import java.io.File
import java.io.FileOutputStream

private const val BACKUP_FILE = "mygiulia_config_backup.properties"
private const val APP_NAME = "MyGiuliaBackup"
private const val TAG = "DriveBackup"

class DriveBackupManager(
    webClientId: String,
    activity: Activity,
) : AuthorizationManager(webClientId, activity) {
    suspend fun exportBackup(file: File) =
        signInAndExecuteAction(
            object : Action {
                override fun execute(token: String) = uploadBackupToDrive(token, file)

                override fun getName() = "exportBackupAction"
            },
        )

    suspend fun restoreBackup(func: (f: File) -> Unit) =
        signInAndExecuteAction(
            object : Action {
                override fun execute(token: String) = downloadBackupFromDrive(token, func)

                override fun getName() = "restoreBackupAction"
            },
        )

    private fun downloadBackupFromDrive(
        accessToken: String,
        func: (f: File) -> Unit,
    ) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val driveService = driveService(accessToken)

                val fileList =
                    driveService
                        .files()
                        .list()
                        .setSpaces("drive")
                        .setQ("name = '$BACKUP_FILE'")
                        .setOrderBy("createdTime desc")
                        .setFields("files(id, createdTime)")
                        .execute()

                if (fileList.files.isNotEmpty()) {
                    Log.d(TAG, "Found (${fileList.files.size}) files with name '$BACKUP_FILE' on GDrive. Taking the newest one")

                    val file = fileList.files[0]
                    Log.d(TAG, "Found file with id: ${file.id} on GDrive. Modification time: ${file.createdTime}")
                    val target = File(activity.filesDir, "restored_backup.json")

                    FileOutputStream(target).use {
                        Log.d(TAG, "Copying remote file ${file.id} into local $target")
                        driveService
                            .files()
                            .get(file.id)
                            .executeMediaAndDownloadTo(it)
                    }

                    Log.d(TAG, "Writing into local $target file finished")
                    func(target)
                    sendBroadcastEvent(BACKUP_RESTORE_SUCCESSFUL)
                } else {
                    Log.d(TAG, "Found 0 files with name '$BACKUP_FILE' on GDrive. Won't restore the backup.")
                    sendBroadcastEvent(BACKUP_RESTORE_NO_FILES)
                }
            } catch (e: GoogleJsonResponseException) {
                if (401 == e.statusCode) {
                    Log.e(TAG, "Token is invalid. Invalidating now...")
                    try {
                        GoogleAuthUtil.clearToken(activity, accessToken)
                    } catch (e1: java.lang.Exception) {
                        Log.e(TAG, "Failed to invalidate the token", e)
                    }
                    sendBroadcastEvent(BACKUP_RESTORE_FAILED)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Restore backup failed", e)
                sendBroadcastEvent(BACKUP_RESTORE_FAILED)
            } finally {
                sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT)
            }
        }
    }

    private fun uploadBackupToDrive(
        accessToken: String,
        configFile: File,
    ) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.i(TAG, "Uploading file ${configFile.absoluteFile} to the drive")
                val driveService = driveService(accessToken)

                val backupFolderId = getOrCreateFolder(driveService)

                val metadata =
                    com.google.api.services.drive.model.File().apply {
                        name = BACKUP_FILE
                        parents = listOf(backupFolderId)
                    }

                val uploadedFile =
                    driveService
                        .files()
                        .create(metadata, FileContent("text/plain", configFile))
                        .setFields("id")
                        .execute()

                Log.i(TAG, "Backup operation completed successfully. File was uploaded. id: ${uploadedFile.id}")

                sendBroadcastEvent(BACKUP_SUCCESSFUL)
            } catch (e: GoogleJsonResponseException) {
                if (401 == e.statusCode) {
                    Log.e(TAG, "Token is invalid. Invalidating now...")
                    try {
                        GoogleAuthUtil.clearToken(activity, accessToken)
                    } catch (e1: java.lang.Exception) {
                        Log.e(TAG, "Failed to invalidate the token", e)
                    }
                    sendBroadcastEvent(BACKUP_FAILED)
                } else {
                    Log.e(TAG, "Upload failed ${e.statusCode}", e)
                    sendBroadcastEvent(BACKUP_FAILED)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed", e)
                sendBroadcastEvent(BACKUP_FAILED)
            } finally {
                sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT)
            }
        }
    }

    private fun driveService(accessToken: String): Drive =
        Drive
            .Builder(
                NetHttpTransport.Builder().build(),
                GsonFactory(),
                credentials(accessToken),
            ).setApplicationName(APP_NAME)
            .build()

    private fun getOrCreateFolder(
        driveService: Drive,
        folderName: String = "mygiulia",
    ): String {
        val result =
            driveService
                .files()
                .list()
                .setQ("mimeType = 'application/vnd.google-apps.folder' and name = '$folderName' and trashed = false")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

        return if (result.files.isNotEmpty()) {
            result.files[0].id
        } else {
            driveService
                .files()
                .create(
                    com.google.api.services.drive.model.File().apply {
                        name = folderName
                        mimeType = "application/vnd.google-apps.folder"
                        parents = listOf("root")
                    },
                ).setFields("id")
                .execute()
                .id
        }
    }
}
