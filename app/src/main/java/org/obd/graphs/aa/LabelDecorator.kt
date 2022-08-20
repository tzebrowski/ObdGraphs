package org.obd.graphs.aa

import android.text.Spannable
import android.text.SpannableString
import androidx.car.app.model.CarColor
import androidx.car.app.model.ForegroundCarColorSpan

internal fun colorize(info: StringBuilder): SpannableString = SpannableString(info).apply {
    setSpan("value=",CarColor.GREEN)
    setSpan("min=",CarColor.GREEN)
    setSpan("max=",CarColor.GREEN)
    setSpan("avg=",CarColor.GREEN)
}

private fun SpannableString.setSpan(txt: String,color: CarColor){

    val i1 = indexOf(txt)
    if (i1 >= 0) {
        setSpan(
            ForegroundCarColorSpan.create(color), i1, i1 + txt.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
}