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

import com.google.api.services.drive.Drive
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.scaleToRange
import org.obd.graphs.bl.trip.TripDescParser
import org.obd.graphs.integrations.gcp.gdrive.DriveHelper.uploadFile
import org.obd.graphs.integrations.log.OutputType
import org.obd.graphs.integrations.log.TripLog
import org.obd.graphs.integrations.log.TripLogTransformer
import org.obd.metrics.pid.PIDsGroup
import java.io.File
import java.util.zip.GZIPOutputStream

internal object TripUpload {

    /**
     * Builds the unified transformer that scales PID values.
     * This is heavy, so it should only be called once per upload batch.
     */
    fun buildTransformer(): TripLogTransformer {
        val registry = DataLoggerRepository.getPidDefinitionRegistry()
        val definitions = registry.findAll()
        val signalsMapper = definitions.associate { it.id.toInt() to it.description.replace("\n", " ") }
        val pidMap = definitions.associateBy { it.id.toInt() }
        // Trips recorded before metadata was skipped may hold the VIN as text; the log viewer expects numbers.
        // findAll() lists live data only.
        val metadata = registry.findBy(PIDsGroup.METADATA).map { it.id.toInt() }.toSet()

        return TripLog.transformer(OutputType.JSON, signalsMapper, metadata) { s, v ->
            if (v is Number) {
                (pidMap[s]?.scaleToRange(v.toFloat())) ?: v
            } else {
                v
            }
        }
    }

    /**
     * Whether a local trip file is due for auto-sync: a finished trip (its name carries the length)
     * not uploaded yet. The trip being recorded has no length in its name yet and is skipped; one
     * that was cut off gets it from tripManager.recoverUnfinishedTrips().
     */
    fun isReadyForSync(
        fileName: String,
        tripDescParser: TripDescParser = TripDescParser()
    ): Boolean =
        fileName.startsWith("trip-") &&
            !fileName.endsWith(".synced") &&
            tripDescParser.decodeTripName(fileName).size > 3

    /**
     * The Drive file name a local trip file is uploaded under.
     */
    fun driveTripFileName(
        deviceId: String,
        localFileName: String
    ): String {
        val originalName = localFileName.removePrefix("trip-profile_")
        return "$deviceId-$originalName.json.gz"
    }

    /**
     * Transforms, compresses, and uploads a single trip file to Google Drive.
     */
    fun Drive.transformAndUploadTrip(
        inFile: File,
        cacheDir: File,
        folderId: String,
        deviceId: String,
        transformer: TripLogTransformer,
        tripDescParser: TripDescParser,
        tags: List<String> = emptyList()
    ) {
        val metadata = mutableMapOf<String, String>()
        val tripDesc = tripDescParser.getTripDesc(inFile.name)
        metadata["trip.duration"] = tripDesc.tripTimeSec
        metadata["trip.profileId"] = tripDesc.profileId
        metadata["trip.startTime"] = tripDesc.startTime
        metadata["trip.profileLabel"] = tripDesc.profileLabel

        val transformedFile = transformer.transform(inFile, metadata)
        val tempGzipFile = File(cacheDir, "${inFile.name}.gz")

        tempGzipFile.outputStream().use { fos ->
            GZIPOutputStream(fos).use { gzipOs ->
                transformedFile.inputStream().use { inputStream ->
                    inputStream.copyTo(gzipOs)
                }
            }
        }

        val fileName = driveTripFileName(deviceId, inFile.name)
        val appProperties = if (tags.isNotEmpty()) mapOf("tags" to TripTags.format(tags)) else null
        this.uploadFile(tempGzipFile, fileName, folderId, "application/gzip", appProperties)

        tempGzipFile.delete()
        transformedFile.delete()
    }
}
