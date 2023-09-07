package org.obd.graphs.aa

import android.graphics.Color
import androidx.car.app.model.CarColor
import org.obd.graphs.ui.common.COLOR_WHITE

internal fun mapColor(color: Int): CarColor {
    return when (color) {
        -1 -> CarColor.PRIMARY
        -48060 -> CarColor.RED
        -6697984 -> CarColor.GREEN
        -17613 -> CarColor.YELLOW
        -16737844 -> CarColor.BLUE

        COLOR_WHITE -> CarColor.PRIMARY
        Color.RED -> CarColor.RED
        Color.BLUE -> CarColor.BLUE
        Color.GREEN -> CarColor.GREEN
        Color.YELLOW -> CarColor.YELLOW
        else -> CarColor.PRIMARY
    }
}
