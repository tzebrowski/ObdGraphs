package org.obd.graphs.ui.preferences.pid

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import org.obd.metrics.pid.PidDefinition


internal fun PidDefinition.displayString(): Spanned {
    val text = "[mode: $mode] $description " +  (if (stable) "" else "(Experimental)")
    return SpannableString(text).apply {
        setSpan(
            RelativeSizeSpan(0.5f), 0, text.indexOf("]") + 1,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (stable){

        }else {
           setSpan(
                ForegroundColorSpan(Color.parseColor("#C22636")),
                text.indexOf("("),
                text.indexOf(")"),
                0
            )
        }
    }
}

