package org.obd.graphs.aa

import android.text.Spannable
import android.text.SpannableString
import androidx.car.app.model.CarColor
import androidx.car.app.model.ForegroundCarColorSpan

internal fun colorize(info: StringBuilder): SpannableString = SpannableString(info).apply {
    val startIndex = indexOf("=") + 1
    val endIndex = indexOf("\n")
    setSpan(
        ForegroundCarColorSpan.create(CarColor.BLUE), startIndex, endIndex,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
}
