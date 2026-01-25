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
            tripTimeSec = p[3]
        )
    }

    fun decodeTripName(fileName: String) = fileName.substring(0, fileName.length - 5).split("-")

}