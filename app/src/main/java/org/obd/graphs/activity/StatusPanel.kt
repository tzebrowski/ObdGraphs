package org.obd.graphs.activity

import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import org.obd.graphs.R
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.profile.PROFILE_NAME_PREFIX
import org.obd.graphs.preferences.profile.getCurrentProfile
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.*

internal fun MainActivity.connectionStatusConnected() {
    updateTextField(
        R.id.connection_status,
        resources.getString(R.string.connection_status),
        "Connected",
       COLOR_PHILIPPINE_GREEN,
        1.1f
    )
}

internal fun MainActivity.connectionStatusDisconnected() {
    updateTextField(
        R.id.connection_status,
        resources.getString(R.string.connection_status),
        "Disconnected",
        COLOR_CARDINAL,
        1.1f
    )
}

internal fun MainActivity.setupStatusPanel() {
    updateTextField(
        R.id.connection_status,
        resources.getString(R.string.connection_status),
        "Disconnected",
        COLOR_CARDINAL,
        1.1f
    )
    updateVehicleProfile()

    (findViewById<TextView>(R.id.vehicle_profile)).let {
        it.setOnClickListener {
            navigateToPreferencesScreen("pref.profiles")
        }
    }

    (findViewById<ImageView>(R.id.toggle_fullscreen)).let {
        it.setOnClickListener {
           sendBroadcastEvent(TOGGLE_TOOLBAR_ACTION)
        }
    }
}

internal fun MainActivity.updateVehicleProfile() {
    updateTextField(
        R.id.vehicle_profile,
        resources.getString(R.string.vehicle_profile),
        Prefs.getString("$PROFILE_NAME_PREFIX.${getCurrentProfile()}", "")!!,
        COLOR_RAINBOW_INDIGO,
        1.2f
    )
}

private fun MainActivity.updateTextField(
    viewId: Int,
    text1: String,
    text2: String,
    color: Int,
    text2Size: Float
) {
    (findViewById<TextView>(viewId)).let {
        it.text = "$text1 $text2"
        it.highLightText(text1, 0.7f, Color.WHITE)
        it.highLightText(text2, text2Size, color)
    }
}