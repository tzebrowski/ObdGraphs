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
package org.obd.graphs.integrations.gcp.authorization

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.tasks.await

private const val TAG = "SilentAuth"

internal object SilentAuthorization {

    suspend fun getAccessTokenSilently(context: Context): String? {
        return try {
            val authorizationClient = Identity.getAuthorizationClient(context)
            val request = AuthorizationRequest.Builder()
                .setRequestedScopes(listOf(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA)))
                .build()

            val result = authorizationClient.authorize(request).await()

            if (result.hasResolution()) {
                Log.w(TAG, "Silent auth failed: UI resolution required.")
                null // We cannot show UI in the background
            } else {
                Log.i(TAG, "Silent auth successful.")
                result.accessToken
            }
        } catch (e: Exception) {
            Log.e(TAG, "Silent auth threw an exception", e)
            null
        }
    }
}
