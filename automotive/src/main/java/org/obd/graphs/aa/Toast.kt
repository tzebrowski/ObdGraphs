package org.obd.graphs.aa

import androidx.car.app.CarContext
import androidx.car.app.CarToast


internal val toast = Toast()

internal class Toast {
    fun show(carCtx: CarContext, id: Int) {
        show(carCtx, carCtx.getString(id))
    }

    fun show(carCtx: CarContext, msg: String) {
        CarToast.makeText(
            carCtx,
            msg, CarToast.LENGTH_LONG
        ).show()
    }

}