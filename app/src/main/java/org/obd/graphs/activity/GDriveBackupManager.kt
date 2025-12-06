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
package org.obd.graphs.activity

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.obd.graphs.R
import java.io.File

private const val TAG = "DriveBackup"
private const val BACKUP_FILE = "mygiulia_config_backup.properties"

class GDriveBackupManager(private val activity: Activity) {

    private val authorizationLauncher = (activity as? ComponentActivity)?.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val token = Identity.getAuthorizationClient(activity)
                .getAuthorizationResultFromIntent(result.data)
                .accessToken

            token?.let {
                val file: String?  = result.data?.getStringExtra("FILE_REF")
                Log.e(TAG, "Uploading file ref $file")
                file?.let {
                    uploadToDrive(token, File(file))
                }
            }
        }
    }

    suspend fun signInAndBackup(fileToUpload: File) {
        try {
            val credentialManager = CredentialManager.create(activity)
            val webClientId = activity.getString(R.string.ANDROID_WEB_CLIENT_ID)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .setFilterByAuthorizedAccounts(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                checkPermissionsAndUpload(fileToUpload)
            } else {
                Log.i(TAG, "Unexpected credential type")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign In Failed", e)
        }
    }

    private fun checkPermissionsAndUpload(configFile: File) {
        Log.i(TAG, "Checking permissions and uploading file")

        val authorizationClient = Identity.getAuthorizationClient(activity)
        val request = AuthorizationRequest.Builder()
            .setRequestedScopes(listOf(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA)))
            .build()

        authorizationClient.authorize(request)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    try {
                        Log.i(TAG, "User must confirm consent screen")

                        val extras = Intent().apply {
                            putExtra("FILE_REF", configFile.absolutePath)
                        }

                        val intentSenderRequest = IntentSenderRequest
                            .Builder(authorizationResult.pendingIntent!!)
                            .setFillInIntent(extras)
                            .build()
                        authorizationLauncher?.launch(intentSenderRequest)

                    } catch (sendEx: IntentSender.SendIntentException) {
                        Log.e(TAG, "Failed to launch consent screen", sendEx)
                    }
                } else {
                    Log.i(TAG, "We already received token, lets upload the file ${authorizationResult.accessToken}")
                    authorizationResult.accessToken?.let {
                        uploadToDrive(it, configFile)
                    }
                }
            }
            .addOnFailureListener { e ->
                if (e is ApiException && e.statusCode == CommonStatusCodes.RESOLUTION_REQUIRED) {
                    try {
                        Log.i(TAG, "Resolution is required")
                        val pendingIntent = e.status.resolution?.intentSender

                        val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent!!)
                            .build()

                        authorizationLauncher?.launch(intentSenderRequest)

                    } catch (sendEx: IntentSender.SendIntentException) {
                        Log.e(TAG, "Failed to launch consent screen", sendEx)
                    }
                } else {
                    Log.e(TAG, "Authorization failed", e)
                }
            }
    }

    private fun uploadToDrive(accessToken: String, configFile: File) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.i(TAG, "Upload to drive $accessToken")
                val credential = HttpRequestInitializer { request ->
                    request.headers.authorization = "Bearer $accessToken"
                }

                val driveService = Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    GsonFactory(),
                    credential
                ).setApplicationName("MyGiuliaBackup").build()

                val metadata = com.google.api.services.drive.model.File().apply {
                    name = BACKUP_FILE
                    parents = listOf("root") // Hidden folder
                }

                val mediaContent = FileContent("text/plain", configFile)
                val uploadedFile = driveService.files().create(metadata, mediaContent)
                    .setFields("id")
                    .execute()

                Log.d(TAG, "Success! File ID: ${uploadedFile.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed", e)
            }
        }
    }
}