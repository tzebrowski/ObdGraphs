package org.obd.graphs.aa

import androidx.car.app.CarContext
import androidx.car.app.CarToast


internal const val LOG_KEY = "AndroidAuto"

fun carToast(carCtx: CarContext, id: Int) {
    carToast(carCtx,carCtx.getString(id))
}

fun carToast(carCtx: CarContext, msg: String) {
    CarToast.makeText(
        carCtx,
        msg, CarToast.LENGTH_LONG
    ).show()
}