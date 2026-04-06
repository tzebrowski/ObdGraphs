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

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.scaleToRange
import org.obd.graphs.bl.trip.TripDescParser
import org.obd.graphs.bl.trip.tripManager
import org.obd.graphs.getContext
import org.obd.graphs.integrations.gcp.authorization.SilentAuthorization
import org.obd.graphs.integrations.gcp.gdrive.DriveHelper.findFolderIdRecursive
import org.obd.graphs.integrations.gcp.gdrive.DriveHelper.uploadFile
import org.obd.graphs.integrations.log.OutputType
import org.obd.graphs.integrations.log.TripLog
import java.io.File
import java.util.zip.GZIPOutputStream

private const val LOG_TAG = "TripCloudSyncWorker"

object DriveSync {
    fun start() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<TripCloudSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(getContext()!!).enqueue(syncRequest)
    }
}

internal class TripCloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext

        val directory = File(tripManager.getTripsDirectory(context))
        val unsyncedFiles = directory.listFiles()?.filter {
            it.name.startsWith("trip-") && !it.name.endsWith(".synced")
        } ?: emptyList()

        if (unsyncedFiles.isEmpty()) {
            return@withContext Result.success()
        }

        val token = SilentAuthorization.getAccessTokenSilently(context)
            ?: return@withContext Result.retry() // Try again later if auth fails

        try {
            val driveService = Drive.Builder(
                NetHttpTransport.Builder().build(),
                GsonFactory(),
                HttpRequestInitializer { request -> request.headers.authorization = "Bearer $token" }
            ).setApplicationName("MyGiuliaBackup").build()

            val folderId = driveService.findFolderIdRecursive("mygiulia/trips")

            val definitions = DataLoggerRepository.getPidDefinitionRegistry().findAll()
            val signalsMapper = definitions.associate { it.id.toInt() to it.description.replace("\n", " ") }
            val pidMap = definitions.associateBy { it.id.toInt() }

            val transformer = TripLog.transformer(OutputType.JSON, signalsMapper) { s, v ->
                if (v is Number) {
                    (pidMap[s]?.scaleToRange(v.toFloat())) ?: v
                } else {
                    v
                }
            }

            val deviceId = Device.id()
            val tripDescParser = TripDescParser()

            unsyncedFiles.forEach { inFile ->
                Log.i(LOG_TAG, "Syncing file: ${inFile.name}")

                val metadata = mutableMapOf<String, String>()
                val tripDesc = tripDescParser.getTripDesc(inFile.name)
                metadata["trip.duration"] = tripDesc.tripTimeSec
                metadata["trip.profileId"] = tripDesc.profileId
                metadata["trip.startTime"] = tripDesc.startTime
                metadata["trip.profileLabel"] = tripDesc.profileLabel

                val transformedFile = transformer.transform(inFile, metadata)
                val tempGzipFile = File(context.cacheDir, "${inFile.name}.gz")

                tempGzipFile.outputStream().use { fos ->
                    GZIPOutputStream(fos).use { gzipOs ->
                        transformedFile.inputStream().use { inputStream ->
                            inputStream.copyTo(gzipOs)
                        }
                    }
                }

                val originalName = inFile.name.removePrefix("trip-profile_")
                val fileName = "$deviceId-$originalName.json.gz"

                driveService.uploadFile(tempGzipFile, fileName, folderId, "application/gzip")

                tempGzipFile.delete()
                transformedFile.delete()

                inFile.renameTo(File(inFile.absolutePath + ".synced"))
            }

            Log.i(LOG_TAG, "Cloud sync complete.")
            Result.success()
        } catch (e: GoogleJsonResponseException) {
            if (e.statusCode == 401) {
                Log.w(LOG_TAG, "Token expired! Clearing cache and retrying...")

                try {
                    GoogleAuthUtil.clearToken(context, token)
                } catch (clearEx: Exception) {
                    Log.e(LOG_TAG, "Failed to clear dead token", clearEx)
                }

                return@withContext Result.retry()
            } else {
                return@withContext Result.retry()
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Upload failed", e)
            Result.retry()
        }
    }
}
