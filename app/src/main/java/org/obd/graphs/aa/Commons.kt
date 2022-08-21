package org.obd.graphs.aa

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import org.obd.graphs.ui.preferences.Prefs
import org.obd.graphs.ui.preferences.getStringSet


internal const val LOG_KEY = "AndroidAuto"

internal fun aaPIDs() =
    Prefs.getStringSet("pref.aa.pids.selected").map { s -> s.toLong() }.toSet()

fun carToast(carCtx: CarContext,id: Int) {
    CarToast.makeText(
        carCtx,
        carCtx.getString(id)
        , CarToast.LENGTH_LONG
    ).show();
}