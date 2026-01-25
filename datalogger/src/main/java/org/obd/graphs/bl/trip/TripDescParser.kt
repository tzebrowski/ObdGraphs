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
package org.obd.graphs.bl.trip

import org.obd.graphs.profile.profile

class TripDescParser {
    fun getTripDesc(fileName: String): TripFileDesc {
        val p = decodeTripName(fileName)
        val profileId = p[1]
        val profiles = profile.getAvailableProfiles()
        val profileLabel = profiles[profileId]!!

        return TripFileDesc(
            fileName = fileName,
            profileId = profileId,
            profileLabel = profileLabel,
            startTime = p[2],
            tripTimeSec = p[3],
        )
    }

    fun decodeTripName(fileName: String) = fileName.substring(0, fileName.length - 5).split("-")
}
