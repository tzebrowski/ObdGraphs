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
package org.obd.graphs.integrations.gdrive

import android.app.Activity
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.obd.graphs.activity.BACKUP_FAILED
import org.obd.graphs.activity.BACKUP_SUCCESSFUL
import org.obd.graphs.integrations.authorization.Action
import org.obd.graphs.integrations.authorization.AuthorizationManager
import org.obd.graphs.runAsync
import org.obd.graphs.sendBroadcastEvent
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

private const val BACKUP_FILE = "mygiulia_config_backup.properties"
private const val APP_NAME = "MyGiuliaBackup"
private const val TAG = "DriveBackup"

class DriveBackupManager(
    private val activity: Activity,
) : AuthorizationManager(activity) {
    suspend fun exportBackup(file: File) =
        signInAndExecuteAction(
            object : Action {
                override fun execute(token: String) = uploadToDrive(token, file)

                override fun getName() = "exportBackupAction"
            },
        )

    suspend fun restoreBackup(func: (f: File) -> Unit) =
        signInAndExecuteAction(
            object : Action {
                override fun execute(token: String) = downloadFromDrive(token, func)

                override fun getName() = "restoreBackupAction"
            },
        )

    private fun downloadFromDrive(
        accessToken: String,
        func: (f: File) -> Unit,
    ) {
        try {
            val driveService = driveService(accessToken)
            runAsync {
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

                    val outputStream: OutputStream = FileOutputStream(target)
                    Log.d(TAG, "Start writing into $target")

                    driveService
                        .files()
                        .get(file.id)
                        .executeMediaAndDownloadTo(outputStream)

                    Log.d(TAG, "Writing into $target finished")
                    func(target)
                    sendBroadcastEvent(BACKUP_SUCCESSFUL)

                } else {
                    Log.d(TAG, "Found 0 files with name '$BACKUP_FILE' on GDrive. Won't restore the backup.")
                }
            }
        } catch (e: GoogleJsonResponseException) {
            if (401 == e.statusCode) {
                Log.e(TAG, "Token is invalid. Invalidating now...")
                try {
                    GoogleAuthUtil.clearToken(activity, accessToken)
                } catch (e1: java.lang.Exception) {
                    Log.e(TAG, "Failed to invalidate the token", e)
                }
                sendBroadcastEvent(BACKUP_FAILED)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Restore backup failed", e)
            sendBroadcastEvent(BACKUP_FAILED)
        }
    }

    private fun uploadToDrive(
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

                val mediaContent = FileContent("text/plain", configFile)
                val uploadedFile =
                    driveService
                        .files()
                        .create(metadata, mediaContent)
                        .setFields("id")
                        .execute()

                Log.i(TAG, "Operation completed. File was uploaded. id: ${uploadedFile.id}")
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
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed", e)
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

    private fun credentials(accessToken: String): HttpRequestInitializer =
        HttpRequestInitializer { request ->
            request.headers.authorization = "Bearer $accessToken"
        }
}
