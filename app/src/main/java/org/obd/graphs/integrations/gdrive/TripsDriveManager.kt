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
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.obd.graphs.SCREEN_UNLOCK_PROGRESS_EVENT
import org.obd.graphs.activity.BACKUP_FAILED
import org.obd.graphs.activity.BACKUP_SUCCESSFUL
import org.obd.graphs.integrations.authorization.Action
import org.obd.graphs.integrations.authorization.AuthorizationManager
import org.obd.graphs.sendBroadcastEvent
import java.io.File

private const val APP_NAME = "MyGiuliaBackup"
private const val TAG = "TripsDriveManager"

class TripsDriveManager(
    private val activity: Activity,
    fragment: Fragment?
) : AuthorizationManager(activity, fragment) {

    suspend fun exportTrips(file: List<File>) =
        signInAndExecuteAction(
            object : Action {
                override fun execute(token: String) = uploadTripsToDrive(token, file)

                override fun getName() = "exportTripsAction"
            },
        )

    private fun uploadTripsToDrive(
        accessToken: String,
        files: List<File>,
    ) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val driveService = driveService(accessToken)
                val backupFolderId = getOrCreateFolder(driveService)

                files.forEach { file ->
                    Log.i(TAG, "Uploading file ${file.absoluteFile} to the drive")
                    val metadata =
                        com.google.api.services.drive.model.File().apply {
                            name = file.name
                            parents = listOf(backupFolderId)
                        }

                    val uploadedFile =
                        driveService
                            .files()
                            .create(metadata, FileContent("text/plain", file))
                            .setFields("id")
                            .execute()

                    Log.i(TAG, "File ${file.name} was uploaded to the drive. id: ${uploadedFile.id}")
                }
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
