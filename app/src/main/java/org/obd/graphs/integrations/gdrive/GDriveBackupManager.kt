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
import android.content.IntentSender
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.obd.graphs.R
import org.obd.graphs.activity.BACKUP_FAILED
import org.obd.graphs.activity.BACKUP_SUCCESSFUL
import org.obd.graphs.sendBroadcastEvent
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


private const val TAG = "DriveBackup"
private const val BACKUP_FILE = "mygiulia_config_backup.properties"
private const val APP_NAME = "MyGiuliaBackup"

interface Action {
    fun getName(): String
    fun execute(token: String)
}

class GDriveBackupManager(
    private val activity: Activity,
) {
    private var currentAction: Action? = null

    private val authorizationLauncher =
        (activity as? ComponentActivity)?.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val token =
                    Identity
                        .getAuthorizationClient(activity)
                        .getAuthorizationResultFromIntent(result.data)
                        .accessToken

                token?.let {
                    currentAction?.let { action ->
                        Log.i(TAG, "User accepted the consent. Executing the action: ${action.getName()}")
                        action.execute(token)
                        currentAction = null
                    }
                }
            }
        }

    suspend fun exportBackup(file: File) {
        val action: Action = object : Action {
            override fun execute(token: String) = uploadToDrive(token, file)
            override fun getName() = "exportBackupAction"
        }
        signInAndExecuteAction(action)
    }

    suspend fun restoreBackup(func: (f: File) -> Unit) {
        val action: Action = object : Action {
            override fun execute(token: String) = downloadFromDrive(token, func)
            override fun getName() = "restoreBackupAction"
        }
        signInAndExecuteAction(action)
    }

    private suspend fun signInAndExecuteAction(action: Action) {

        try {

            Log.i(TAG, "Start executing action: ${action.getName()}")

            val credentialManager = CredentialManager.create(activity)
            val webClientId = activity.getString(R.string.ANDROID_WEB_CLIENT_ID)

            val googleIdOption =
                GetGoogleIdOption
                    .Builder()
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .setFilterByAuthorizedAccounts(false)
                    .build()

            val request =
                GetCredentialRequest
                    .Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                checkPermissionsAndExecuteAction(action)
            } else {
                Log.i(TAG, "Unexpected credential type")
            }

            Log.i(TAG, "Finished executing action: ${action.getName()}")

        } catch (e: Exception) {
            Log.i(TAG, "Failed executing action: ${action.getName()}", e)
        }
    }

    private fun checkPermissionsAndExecuteAction(action: Action) {
        Log.i(TAG, "Checking permissions and executing action: ${action.getName()}")

        val authorizationClient = Identity.getAuthorizationClient(activity)
        val request =
            AuthorizationRequest
                .Builder()
                .setRequestedScopes(listOf(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA)))
                .build()

        authorizationClient
            .authorize(request)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    try {
                        Log.i(TAG, "User must confirm consent screen")
                        val intentSenderRequest =
                            IntentSenderRequest
                                .Builder(authorizationResult.pendingIntent!!)
                                .build()

                        currentAction = action

                        authorizationLauncher?.launch(intentSenderRequest)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Log.e(TAG, "Failed to launch consent screen", sendEx)
                    }
                } else {
                    Log.i(TAG, "We already received token, executing the action ${authorizationResult.accessToken}")
                    Log.i(TAG, "Granted scopes: ${authorizationResult.grantedScopes}")
                    authorizationResult.accessToken?.let {
                        action.execute(it)
                    }
                }
            }.addOnFailureListener { e ->
                if (e is ApiException && e.statusCode == CommonStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        Log.i(TAG, "Resolution is required")
                        val pendingIntent = e.status.resolution?.intentSender

                        val intentSenderRequest =
                            IntentSenderRequest
                                .Builder(pendingIntent!!)
                                .build()

                        currentAction = action

                        authorizationLauncher?.launch(intentSenderRequest)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Log.e(TAG, "Failed to launch consent screen", sendEx)
                    }
                } else {
                    Log.e(TAG, "Authorization failed", e)
                }
            }
    }

    private fun downloadFromDrive(
        accessToken: String,
        func: (f: File) -> Unit,
    ) {
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
                Log.d(TAG, "Found (${fileList.files.size}) files with name '${BACKUP_FILE}' on GDrive. Taking the newest one")

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
                Log.d(TAG, "Found 0 files with name '${BACKUP_FILE}' on GDrive. Won't restore the backup.")
            }
        } catch (e: GoogleJsonResponseException) {
            if (401 == e.statusCode) {
                Log.e(TAG, "Token is invalid. Invalidating now...")
                try {
                    GoogleAuthUtil.invalidateToken(activity, accessToken)
                } catch (e1: java.lang.Exception) {
                    Log.e(TAG, "Failed to invalidate the token", e)
                }
                sendBroadcastEvent(BACKUP_FAILED)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Restore backup failed", e)
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
                        GoogleAuthUtil.invalidateToken(activity, accessToken)
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
        Drive.Builder(
                NetHttpTransport.Builder().build(),
                GsonFactory(),
                credentials(accessToken),
            ).setApplicationName(APP_NAME)
            .build()

    private fun credentials(accessToken: String): HttpRequestInitializer =
        HttpRequestInitializer { request ->
            request.headers.authorization = "Bearer $accessToken"
        }

    private fun getOrCreateFolder(
        driveService: Drive,
        folderName: String = "mygiulia",
    ): String {
        val query = "mimeType = 'application/vnd.google-apps.folder' and name = '$folderName' and trashed = false"
        val result =
            driveService
                .files()
                .list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

        if (result.files.isNotEmpty()) {
            return result.files[0].id
        } else {
            val folderMetadata =
                com.google.api.services.drive.model.File().apply {
                    name = folderName
                    mimeType = "application/vnd.google-apps.folder"
                    parents = listOf("root")
                }
            return driveService
                .files()
                .create(folderMetadata)
                .setFields("id")
                .execute()
                .id
        }
    }
}
