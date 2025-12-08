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
package org.obd.graphs.integrations.authorization

import android.app.Activity
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
import com.google.api.services.drive.DriveScopes
import org.obd.graphs.R
import org.obd.graphs.SCREEN_LOCK_PROGRESS_EVENT
import org.obd.graphs.sendBroadcastEvent

 private const val TAG = "AuthorizationManager"

interface Action {
    fun getName(): String

    fun execute(token: String)
}

abstract class AuthorizationManager(
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
                        sendBroadcastEvent(SCREEN_LOCK_PROGRESS_EVENT)
                         action.execute(token)
                        currentAction = null
                    }
                }
            }
        }

    protected suspend fun signInAndExecuteAction(action: Action) {
        try {
            val webClientId = activity.getString(R.string.ANDROID_WEB_CLIENT_ID)
            Log.i(TAG, "Start executing action: ${action.getName()} for client.id=$webClientId")
            val credentialManager = CredentialManager.create(activity)

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
                        currentAction = action

                        authorizationLauncher?.launch( IntentSenderRequest
                            .Builder(authorizationResult.pendingIntent!!)
                            .build())
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Log.e(TAG, "Failed to launch consent screen", sendEx)
                    }
                } else {
                    Log.i(TAG, "We already received token, executing the action ${authorizationResult.accessToken}")
                    Log.i(TAG, "Granted scopes: ${authorizationResult.grantedScopes}")
                    authorizationResult.accessToken?.let {
                        sendBroadcastEvent(SCREEN_LOCK_PROGRESS_EVENT)
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
}
