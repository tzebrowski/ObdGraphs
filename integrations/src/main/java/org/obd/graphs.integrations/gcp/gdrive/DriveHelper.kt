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
package org.obd.graphs.integrations.gcp.gdrive

import android.util.Log
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import java.io.File
import com.google.api.services.drive.model.File as DriveFile

private const val TAG = "DriveHelper"

internal object DriveHelper {

    fun Drive.findFolderIdRecursive(path: String): String {
        val folderNames = path.split("/").filter { it.isNotEmpty() }
        var currentParentId = "root"
        for (folderName in folderNames) {
            currentParentId = findOrCreateSingleFolder(folderName, currentParentId)
        }
        return currentParentId
    }

    fun Drive.findOrCreateSingleFolder(folderName: String, parentId: String): String {
        val query = "mimeType = 'application/vnd.google-apps.folder' and name = '$folderName' and '$parentId' in parents and trashed = false"

        val files = this.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()
            .files

        return if (files.isNotEmpty()) {
            files.first().id
        } else {
            val metadata = DriveFile().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf(parentId)
            }
            this.files().create(metadata).setFields("id").execute().id
        }
    }

    fun Drive.uploadFile(
        localFile: File,
        fileName: String,
        parentFolderId: String,
        mimeType: String = "text/plain"
    ): DriveFile {
        Log.i(TAG, "Uploading file ${localFile.absolutePath} to $fileName")
        val metadata = DriveFile().apply {
            name = fileName
            parents = listOf(parentFolderId)
        }
        val content = FileContent(mimeType, localFile)

        val uploaded = this.files().create(metadata, content).setFields("id").execute()

        Log.i(TAG, "Uploaded ${localFile.name}, ID: ${uploaded.id} as $fileName")
        return uploaded
    }
}
