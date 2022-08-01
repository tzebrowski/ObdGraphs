package org.obd.graphs.ui.preferences.pid

import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import org.obd.graphs.bl.datalogger.defaultPidFiles
import org.obd.graphs.ui.common.COLOR_CARDINAL
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.metrics.pid.PidDefinition

internal fun PidDefinition.displayString(): Spanned {
    val text = "[${defaultPidFiles[resourceFile]?: resourceFile}] ${longDescription?:description} " +  (if (stable) "" else "(Experimental)")
    return SpannableString(text).apply {
        var endIndexOf = text.indexOf("]") + 1
        setSpan(
            RelativeSizeSpan(0.5f), 0, endIndexOf,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        setSpan(
            ForegroundColorSpan(COLOR_PHILIPPINE_GREEN), 0, endIndexOf,
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
                ForegroundColorSpan(COLOR_CARDINAL),
                startIndexOf,
                endIndexOf,
                0
            )
        }
    }
}



