package org.obd.graphs.aa.screen

import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat

fun createAction(carContext: CarContext, iconResId: Int, iconColorTint: CarColor, func: () -> Unit): Action =
    Action.Builder()
        .setIcon(
            CarIcon.Builder(
                IconCompat.createWithResource(
                    carContext,
                    iconResId
                )
            ).setTint(iconColorTint).build()
        )
        .setOnClickListener {
            func()
        }.build()