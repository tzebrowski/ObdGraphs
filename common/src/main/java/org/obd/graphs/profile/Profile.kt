package org.obd.graphs.profile

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getString

const val PROFILE_ID_PREF = "pref.profile.id"
const val PROFILE_NAME_PREFIX = "pref.profile.names"
const val DEFAULT_MAX_PROFILES = 10

fun getProfiles() =
    (1..DEFAULT_MAX_PROFILES)
        .associate {
            "profile_$it" to Prefs.getString(
                "$PROFILE_NAME_PREFIX.profile_$it",
                "Profile $it"
            )
        }


fun getSelectedProfile(): String = Prefs.getString(PROFILE_ID_PREF)!!
