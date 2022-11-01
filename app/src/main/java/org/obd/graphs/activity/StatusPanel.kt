package org.obd.graphs.activity

import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.profile.PROFILE_NAME_PREFIX
import org.obd.graphs.preferences.profile.vehicleProfile
import org.obd.graphs.sendBroadcastEvent
import org.obd.graphs.ui.common.*


internal fun MainActivity.updateAdapterConnectionType() {
    updateTextField(
        R.id.connection_status,
        resources.getString(R.string.adapter_connection_type),
        dataLoggerPreferences.instance.connectionType,
        COLOR_PHILIPPINE_GREEN,
        1.0f
    )
}

internal fun MainActivity.setupStatusPanel() {
    updateAdapterConnectionType()
    updateVehicleProfile()

    (findViewById<TextView>(R.id.connection_status)).let {
        it.setOnClickListener {
            navigateToPreferencesScreen("pref.adapter.connection")
        }
    }

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
        Prefs.getString("$PROFILE_NAME_PREFIX.${vehicleProfile.getCurrentProfile()}", "")!!,
        COLOR_RAINBOW_INDIGO,
        1.0f
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