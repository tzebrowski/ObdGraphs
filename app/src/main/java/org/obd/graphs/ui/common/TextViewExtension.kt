package org.obd.graphs.ui.common

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.widget.TextView

fun TextView.setText(it: String?, color: Int, size: Float) {

    var valText: String? = it
    if (valText == null) {
        valText = ""
    }

    val valSpanString = SpannableString(valText)
    valSpanString.setSpan(RelativeSizeSpan(size), 0, valSpanString.length, 0) // set size
    valSpanString.setSpan(StyleSpan(Typeface.BOLD), 0, valSpanString.length, 0)
    valSpanString.setSpan(
        ForegroundColorSpan(color),
        0,
        valSpanString.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    text = valSpanString
}

fun TextView.highLightText(textToHighlight: String, size: Float, color: Int) {
    val tvt = text.toString()
    var ofe = tvt.indexOf(textToHighlight, 0)
    val wordToSpan: Spannable = SpannableString(text)
    var ofs = 0
    while (ofs < tvt.length && ofe != -1) {
        ofe = tvt.indexOf(textToHighlight, ofs)
        if (ofe == -1) break else {
            wordToSpan.setSpan(
                RelativeSizeSpan(size),
                ofe,
                ofe + textToHighlight.length,
                0
            ) // set size
            wordToSpan.setSpan(
                ForegroundColorSpan(color),
                ofe,
                ofe + textToHighlight.length,
                0
            ) // set color
            setText(wordToSpan, TextView.BufferType.SPANNABLE)
        }
        ofs = ofe + 1
    }
}