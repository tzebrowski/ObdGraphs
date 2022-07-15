package org.obd.graphs.ui.preferences

import android.text.Html
import android.text.Spanned
import org.obd.metrics.pid.PidDefinition

fun PidDefinition.displayString(): Spanned =
    Html.fromHtml(getTextSize("[$mode] ",22) + description + (if (stable) "" else "\n<font color=\"#ff0000\">(Experimental)</font>"))

private fun getTextSize(text: String, size: Int): String? {
    return "<span style=\"size:$size\">$text</span>"
}