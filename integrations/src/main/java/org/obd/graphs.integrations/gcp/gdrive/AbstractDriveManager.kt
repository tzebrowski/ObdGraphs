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
import androidx.fragment.app.Fragment
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import org.obd.graphs.integrations.gcp.authorization.AuthorizationManager

private const val APP_NAME = "MyGiuliaBackup"

internal abstract class AbstractDriveManager(
    webClientId: String,
    activity: Activity,
    fragment: Fragment?,
) : AuthorizationManager(webClientId, activity, fragment) {
    protected fun driveService(accessToken: String): Drive =
        Drive
            .Builder(
                NetHttpTransport.Builder().build(),
                GsonFactory(),
                credentials(accessToken),
            ).setApplicationName(APP_NAME)
            .build()

    protected fun getOrCreateFolderStructure(
        driveService: Drive,
        folderPath: String,
    ): String {
        val folderNames = folderPath.split("/").filter { it.isNotEmpty() }
        var currentParentId = "root"
        for (folderName in folderNames) {
            currentParentId = findOrCreateSingleFolder(driveService, folderName, currentParentId)
        }
        return currentParentId
    }

    protected fun findOrCreateSingleFolder(
        driveService: Drive,
        folderName: String,
        parentId: String,
    ): String {
        val query =
            "mimeType = 'application/vnd.google-apps.folder' " +
                "and name = '$folderName' " +
                "and '$parentId' in parents " +
                "and trashed = false"

        val result =
            driveService
                .files()
                .list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

        return if (result.files.isNotEmpty()) {
            result.files[0].id
        } else {
            val fileMetadata =
                com.google.api.services.drive.model.File().apply {
                    name = folderName
                    mimeType = "application/vnd.google-apps.folder"
                    parents = listOf(parentId)
                }

            driveService
                .files()
                .create(fileMetadata)
                .setFields("id")
                .execute()
                .id
        }
    }
}
