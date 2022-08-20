package org.obd.graphs.android.car

import android.text.Spannable
import android.text.SpannableString
import androidx.car.app.model.CarColor
import androidx.car.app.model.ForegroundCarColorSpan

fun colorize(info: StringBuilder): SpannableString = SpannableString(info).apply {
    var txt = "value="
    var i = indexOf(txt)

    setSpan(
        ForegroundCarColorSpan.create(CarColor.GREEN)
        , i, i + txt.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    txt = "min="
    i = indexOf(txt)

    setSpan(
        ForegroundCarColorSpan.create(CarColor.GREEN), i, i + txt.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )


    txt = "max="
    i = indexOf(txt)

    setSpan(
        ForegroundCarColorSpan.create(CarColor.GREEN), i, i + txt.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    txt = "avg="
    i = indexOf(txt)

    setSpan(
        ForegroundCarColorSpan.create(CarColor.GREEN), i, i + txt.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
}