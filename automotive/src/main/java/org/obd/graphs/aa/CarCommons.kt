package org.obd.graphs.aa

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getStringSet


internal const val LOG_KEY = "AndroidAuto"

fun carToast(carCtx: CarContext,id: Int) {
    CarToast.makeText(
        carCtx,
        carCtx.getString(id)
        , CarToast.LENGTH_LONG
    ).show();
}