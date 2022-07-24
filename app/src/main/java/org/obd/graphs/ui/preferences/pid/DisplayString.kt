package org.obd.graphs.ui.preferences.pid

import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.defaultPidFiles
import org.obd.graphs.ui.common.color
import org.obd.metrics.pid.PidDefinition

internal fun PidDefinition.displayString(): Spanned {
    val text = "[${defaultPidFiles[resourceFile]?: resourceFile}] $description " +  (if (stable) "" else "(Experimental)")
    return SpannableString(text).apply {
        var endIndexOf = text.indexOf("]") + 1
        setSpan(
            RelativeSizeSpan(0.5f), 0, endIndexOf,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        setSpan(
            ForegroundColorSpan(color(R.color.philippine_green)), 0, endIndexOf,
            0
        )

        if (!stable){
            endIndexOf = text.indexOf(")") + 1
            val startIndexOf = text.indexOf("(")
            setSpan(
                RelativeSizeSpan(0.5f), startIndexOf, endIndexOf,
               0
            )

            setSpan(
                ForegroundColorSpan(color(R.color.cardinal)),
                startIndexOf,
                endIndexOf,
                0
            )
        }
    }
}



