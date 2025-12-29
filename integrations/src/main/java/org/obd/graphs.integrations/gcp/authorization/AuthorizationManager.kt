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
package org.obd.graphs.integrations.gcp.authorization

import android.app.Activity
import android.content.IntentSender
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.obd.graphs.SCREEN_LOCK_PROGRESS_EVENT
import org.obd.graphs.sendBroadcastEvent

private const val TAG = "AuthorizationManager"

internal typealias AuthenticatedAction = suspend (accessToken: String) -> Unit

internal abstract class AuthorizationManager(
    private val webClientId: String,
    protected val activity: Activity,
    private val fragment: Fragment? = null,
) {
    private var pendingAction: AuthenticatedAction? = null
    private var pendingActionName: String = ""

    abstract fun getScopes(): List<Scope>

    private val lifecycleScope: CoroutineScope?
        get() = fragment?.lifecycleScope ?: (activity as? LifecycleOwner)?.lifecycleScope

    private val authorizationLauncher =
        (fragment ?: (activity as? ComponentActivity))?.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            handleActivityResult(result)
        }

    protected suspend fun signInAndExecute(
        authenticatedActionName: String,
        authenticatedAction: AuthenticatedAction,
    ) {
        try {
            Log.i(TAG, "Start executing action: $authenticatedActionName for client.id=$webClientId")
            val credentialManager = CredentialManager.create(activity)

            val googleIdOption =
                GetGoogleIdOption
                    .Builder()
                    .setServerClientId(webClientId)
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
                checkPermissionsAndExecuteAction(authenticatedActionName, authenticatedAction)
            } else {
                Log.w(TAG, "Unexpected credential type: ${credential.type}")
            }
        } catch (e: NoCredentialException) {
            Log.e(TAG, "User has no credentials saved. Redirecting to login...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed executing action: $authenticatedActionName", e)
        }
    }

    private fun checkPermissionsAndExecuteAction(
        authenticatedActionName: String,
        authenticatedAction: AuthenticatedAction,
    ) {
        val scopes = getScopes()
        Log.i(TAG, "Checking permissions for scopes: $scopes and executing action: $authenticatedActionName")

        val authorizationClient = Identity.getAuthorizationClient(activity)
        val request =
            AuthorizationRequest
                .Builder()
                .setRequestedScopes(scopes)
                .build()

        authorizationClient
            .authorize(request)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    launchConsentScreen(result.pendingIntent?.intentSender, authenticatedActionName, authenticatedAction)
                } else {
                    val token = result.accessToken
                    if (token != null) {
                        Log.i(TAG, "Token received, executing action: $authenticatedActionName")
                        executeActionSafely(token, authenticatedAction)
                    }
                }
            }.addOnFailureListener { e ->
                if (e is ApiException && e.statusCode == CommonStatusCodes.RESOLUTION_REQUIRED) {
                    launchConsentScreen(e.status.resolution?.intentSender, authenticatedActionName, authenticatedAction)
                } else {
                    Log.e(TAG, "Authorization failed", e)
                }
            }
    }

    private fun launchConsentScreen(
        intentSender: IntentSender?,
        authenticatedActionName: String,
        authenticatedAction: AuthenticatedAction,
    ) {
        try {
            Log.i(TAG, "Launching consent screen for $authenticatedActionName")
            if (intentSender == null) return

            pendingAction = authenticatedAction
            pendingActionName = authenticatedActionName

            authorizationLauncher?.launch(
                IntentSenderRequest.Builder(intentSender).build(),
            )
        } catch (e: IntentSender.SendIntentException) {
            Log.e(TAG, "Failed to launch consent screen", e)
        }
    }

    private fun handleActivityResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val token =
                Identity
                    .getAuthorizationClient(activity)
                    .getAuthorizationResultFromIntent(result.data)
                    .accessToken

            if (token != null && pendingAction != null) {
                Log.i(TAG, "Consent granted. Executing pending action: $pendingActionName")
                executeActionSafely(token, pendingAction!!)
            }
            pendingAction = null
            pendingActionName = ""
        } else {
            Log.w(TAG, "Authorization result not OK: ${result.resultCode}")
        }
    }

    private fun executeActionSafely(
        token: String,
        authenticatedAction: AuthenticatedAction,
    ) {
        sendBroadcastEvent(SCREEN_LOCK_PROGRESS_EVENT)
        val scope = lifecycleScope
        if (scope == null) {
            Log.e(TAG, "Cannot execute action: Host is not a LifecycleOwner")
        } else {
            scope.launch {
                authenticatedAction(token)
            }
        }
    }
}
