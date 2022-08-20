package org.obd.graphs.aa

import android.text.Spannable
import android.text.SpannableString
import androidx.car.app.model.CarColor
import androidx.car.app.model.ForegroundCarColorSpan

internal fun colorize(info: StringBuilder): SpannableString = SpannableString(info).apply {
    var txt = "value="
    var i = indexOf(txt)

    setSpan(
        ForegroundCarColorSpan.create(CarColor.GREEN)
        , i, i + txt.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )

    txt = "min="
    i = indexOf(txt)

    if (i > 0) {
   setSpan(
            ForegroundCarColorSpan.create(CarColor.GREEN), i, i + txt.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    txt = "max="
    i = indexOf(txt)
    if (i > 0) {
        setSpan(
            ForegroundCarColorSpan.create(CarColor.GREEN), i, i + txt.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }


    txt = "avg="
    i = indexOf(txt)
    if (i > 0) {
        setSpan(
            ForegroundCarColorSpan.create(CarColor.GREEN), i, i + txt.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}