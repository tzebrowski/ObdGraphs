 /**
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

import android.app.Activity
import androidx.fragment.app.Fragment
import org.obd.graphs.SCREEN_UNLOCK_PROGRESS_EVENT
import org.obd.graphs.TRIPS_UPLOAD_FAILED
import org.obd.graphs.TRIPS_UPLOAD_NO_FILES_SELECTED
import org.obd.graphs.TRIPS_UPLOAD_SUCCESSFUL
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.datalogger.scaleToRange
import org.obd.graphs.bl.trip.TripDescParser
import org.obd.graphs.integrations.log.OutputType
import org.obd.graphs.integrations.log.TripLog
import org.obd.graphs.sendBroadcastEvent
import java.io.File

internal open class DefaultTripLogDriveManager(
    webClientId: String,
    activity: Activity,
    fragment: Fragment?,
) : AbstractDriveManager(webClientId, activity, fragment),
    TripLogDriveManager {
    override suspend fun exportTrips(files: List<File>) =
        signInAndExecute("exportTrips") { token ->
            executeDriveOperation(
                accessToken = token,
                onFailure = { sendBroadcastEvent(TRIPS_UPLOAD_FAILED) },
                onFinally = { sendBroadcastEvent(SCREEN_UNLOCK_PROGRESS_EVENT) },
            ) { drive ->
                if (files.isEmpty()) {
                    sendBroadcastEvent(TRIPS_UPLOAD_NO_FILES_SELECTED)
                } else {
                    val folderId = drive.findFolderIdRecursive("mygiulia/trips")
                    val definitions = DataLoggerRepository.getPidDefinitionRegistry().findAll()
                    val signalsMapper = definitions.associate { it.id.toInt() to it.description.replace("\n", " ") }
                    val pidMap = definitions.associateBy { it.id.toInt() }
                    val transformer =
                        TripLog.transformer(OutputType.JSON, signalsMapper) { s, v ->
                            if (v is Number) {
                                (pidMap[s]?.scaleToRange(v.toFloat())) ?: v
                            } else {
                                v
                            }
                         }

                    val deviceId = Device.id()
                    files.forEach { inFile ->

                        val metadata = mutableMapOf<String,String>()
                        val tripDesc = TripDescParser().getTripDesc(inFile.name)
                        metadata["trip.duration"] = tripDesc.tripTimeSec
                        metadata["trip.profileId"] = tripDesc.profileId
                        metadata["trip.startTime"] = tripDesc.startTime
                        metadata["trip.profileLabel"] = tripDesc.profileLabel
                        metadata["trip.profileId"] = tripDesc.profileId

                        transformer.transform(inFile,metadata).inputStream().use { outFile ->
                            drive.uploadFile(
                                InputStreamContent(
                                    "text/plain",
                                    outFile,
                                    "$deviceId-${inFile.name.removePrefix("trip-profile_")}",
                                ),
                                folderId,
                            )
                        }
                    }
                    sendBroadcastEvent(TRIPS_UPLOAD_SUCCESSFUL)
                }
            }
        }
}
