package org.obd.graphs.activity

import android.graphics.Color
import android.widget.TextView
import org.obd.graphs.R
import org.obd.graphs.ui.common.color
import org.obd.graphs.ui.common.highLightText
import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.profile.PROFILE_NAME_PREFIX
import org.obd.graphs.ui.preferences.profile.getCurrentProfile

private fun MainActivity.updateTextField(
    id: Int,
    t1: String,
    t2: String,
    t2_color: Int,
    t2_size: Float
) {
    (findViewById<TextView>(id)).let {
        it.text = "$t1 $t2"
        it.highLightText(t1, 0.7f, Color.WHITE)
        it.highLightText(t2, t2_size, color(t2_color))
    }
}

internal fun MainActivity.connectionStatusConnected() {
    updateTextField(
        R.id.connection_status,
        resources.getString(R.string.connection_status),
        "Connected",
        R.color.philippine_green,
        1.1f
    )
}

internal fun MainActivity.connectionStatusDiconnected() {
    updateTextField(
        R.id.connection_status,
        resources.getString(R.string.connection_status),
        "Disconnected",
        R.color.cardinal,
        1.1f
    )
}


internal fun MainActivity.initiateStatusPanel() {
    updateTextField(
        R.id.connection_status,
        resources.getString(R.string.connection_status),
        "Disconnected",
        R.color.cardinal,
        1.1f
    )
    updateVehicleProfile()
}

internal fun MainActivity.updateVehicleProfile() {
    updateTextField(
        R.id.vehicle_profile,
        resources.getString(R.string.vehicle_profile),
        Prefs.getString("$PROFILE_NAME_PREFIX.${getCurrentProfile()}", "")!!,
        R.color.rainbow_indigo,
        1.2f
    )
}
