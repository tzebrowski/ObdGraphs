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
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.obd.graphs.integrations.gcp.authorization.AuthorizationManager
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import com.google.api.services.drive.model.File as DriveFile

private const val APP_NAME = "MyGiuliaBackup"
private const val TAG = "AbstractDriveManager"

internal class InputStreamContent(
    type: String?,
    private val content: InputStream,
    val fileName: String,
) : AbstractInputStreamContent(type) {
    override fun getLength(): Long = content.available().toLong()

    override fun retrySupported(): Boolean = true

    @Throws(FileNotFoundException::class)
    override fun getInputStream(): InputStream = content

    override fun setType(type: String): InputStreamContent = super.setType(type) as InputStreamContent

    override fun setCloseInputStream(closeInputStream: Boolean): InputStreamContent =
        super.setCloseInputStream(closeInputStream) as InputStreamContent
}

internal abstract class AbstractDriveManager(
    webClientId: String,
    activity: Activity,
    fragment: Fragment?,
) : AuthorizationManager(webClientId, activity, fragment) {
    override fun getScopes(): List<Scope> = listOf(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))

    /**
     * Executes a block using the Drive service, handling 401 invalidation and IO context switching.
     */
    open suspend fun <T> executeDriveOperation(
        accessToken: String,
        onFailure: () -> Unit,
        onFinally: () -> Unit,
        block: suspend (Drive) -> T,
    ) {
        withContext(Dispatchers.IO) {
            try {
                val driveService =
                    Drive
                        .Builder(
                            NetHttpTransport.Builder().build(),
                            GsonFactory(),
                            credentials(accessToken),
                        ).setApplicationName(APP_NAME)
                        .build()

                block(driveService)
            } catch (e: GoogleJsonResponseException) {
                if (e.statusCode == 401) {
                    Log.e(TAG, "Token is invalid (401). Invalidating cache...")
                    try {
                        GoogleAuthUtil.clearToken(activity, accessToken)
                    } catch (clearEx: Exception) {
                        Log.e(TAG, "Failed to invalidate token", clearEx)
                    }
                    onFailure()
                } else {
                    Log.e(TAG, "Drive API error: ${e.statusCode}", e)
                    onFailure()
                }
            } catch (e: Exception) {
                Log.e(TAG, "General Drive error", e)
                onFailure()
            } finally {
                onFinally()
            }
        }
    }

    fun Drive.uploadFile(
        content: InputStreamContent,
        parentFolderId: String,
    ): DriveFile {
        Log.i(TAG, "Uploading file ${content.fileName}")
        val metadata =
            DriveFile().apply {
                name = content.fileName
                parents = listOf(parentFolderId)
            }

        val uploaded =
            this
                .files()
                .create(metadata, content)
                .setFields("id")
                .execute()

        Log.i(TAG, "Uploaded ${content.fileName}, ID: ${uploaded.id}")
        return uploaded
    }

    fun Drive.uploadFile(
        localFile: File,
        fileName: String,
        parentFolderId: String,
        mimeType: String = "text/plain",
    ): DriveFile {
        Log.i(TAG, "Uploading file ${localFile.absolutePath} to $fileName")
        val metadata =
            DriveFile().apply {
                name = fileName
                parents = listOf(parentFolderId)
            }
        val content = FileContent(mimeType, localFile)

        val uploaded =
            this
                .files()
                .create(metadata, content)
                .setFields("id")
                .execute()

        Log.i(TAG, "Uploaded ${localFile.name}, ID: ${uploaded.id} as $fileName")
        return uploaded
    }

    protected fun Drive.findFolderIdRecursive(path: String): String {
        val folderNames = path.split("/").filter { it.isNotEmpty() }
        var currentParentId = "root"
        for (folderName in folderNames) {
            currentParentId = findOrCreateSingleFolder(this, folderName, currentParentId)
        }
        return currentParentId
    }

    private fun findOrCreateSingleFolder(
        drive: Drive,
        folderName: String,
        parentId: String,
    ): String {
        val query =
            "mimeType = 'application/vnd.google-apps.folder' and name = '$folderName' and '$parentId' in parents and trashed = false"

        val files =
            drive
                .files()
                .list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()
                .files

        return if (files.isNotEmpty()) {
            files.first().id
        } else {
            val metadata =
                DriveFile().apply {
                    name = folderName
                    mimeType = "application/vnd.google-apps.folder"
                    parents = listOf(parentId)
                }
            drive
                .files()
                .create(metadata)
                .setFields("id")
                .execute()
                .id
        }
    }

    private fun credentials(accessToken: String): HttpRequestInitializer =
        HttpRequestInitializer { request ->
            request.headers.authorization = "Bearer $accessToken"
        }
}
