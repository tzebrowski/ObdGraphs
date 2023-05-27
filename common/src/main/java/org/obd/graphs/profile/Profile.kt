package org.obd.graphs.profile

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getString

private const val PROFILE_ID_PREF = "pref.profile.id"
internal const val MAX_PROFILES_PREF = "pref.profile.max_profiles"
internal const val PROFILE_NAME_PREFIX = "pref.profile.names"
private const val DEFAULT_MAX_PROFILES = "6"

fun getProfileList() =
    (1..Prefs.getString(MAX_PROFILES_PREF, DEFAULT_MAX_PROFILES)!!.toInt())
        .associate {
            "profile_$it" to Prefs.getString(
                "$PROFILE_NAME_PREFIX.profile_$it",
                "Profile $it"
            )
        }


internal fun getCurrentProfile(): String = Prefs.getString(PROFILE_ID_PREF)!!
