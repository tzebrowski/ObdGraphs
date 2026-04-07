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
import java.io.File
import java.util.zip.GZIPOutputStream

internal object TripUpload {

    /**
     * Builds the unified transformer that scales PID values.
     * This is heavy, so it should only be called once per upload batch.
     */
    fun buildTransformer(): TripLogTransformer {
        val definitions = DataLoggerRepository.getPidDefinitionRegistry().findAll()
        val signalsMapper = definitions.associate { it.id.toInt() to it.description.replace("\n", " ") }
        val pidMap = definitions.associateBy { it.id.toInt() }

        return TripLog.transformer(OutputType.JSON, signalsMapper) { s, v ->
            if (v is Number) {
                (pidMap[s]?.scaleToRange(v.toFloat())) ?: v
            } else {
                v
            }
        }
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
        tripDescParser: TripDescParser
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

        val originalName = inFile.name.removePrefix("trip-profile_")
        val fileName = "$deviceId-$originalName.json.gz"
        this.uploadFile(tempGzipFile, fileName, folderId, "application/gzip")

        tempGzipFile.delete()
        transformedFile.delete()
    }
}
