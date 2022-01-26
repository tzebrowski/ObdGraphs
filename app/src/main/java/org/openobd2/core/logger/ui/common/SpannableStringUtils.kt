package org.openobd2.core.logger.ui.common

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.widget.TextView

class SpannableStringUtils {
    companion object {
        @JvmStatic
        fun generate(it: String?, color: Int, size: Float): SpannableString {

            var valText: String? = it
            if (valText == null) {
                valText = ""
            }

            val valSpanString = SpannableString(valText)
            valSpanString.setSpan(RelativeSizeSpan(size), 0, valSpanString.length, 0) // set size
            //valSpanString.setSpan(UnderlineSpan(), 0, valSpanString.length, 0)
            valSpanString.setSpan(StyleSpan(Typeface.BOLD), 0, valSpanString.length, 0)
            //valSpanString.setSpan(StyleSpan(Typeface.ITALIC), 0, valSpanString.length, 0)
            valSpanString.setSpan(
                ForegroundColorSpan(color),
                0,
                valSpanString.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return valSpanString
        }
        fun setHighLightedText(tv: TextView, textToHighlight: String, size: Float, color: Int) {
            val tvt = tv.text.toString()
            var ofe = tvt.indexOf(textToHighlight, 0)
            val wordToSpan: Spannable = SpannableString(tv.text)
            var ofs = 0
            while (ofs < tvt.length && ofe != -1) {
                ofe = tvt.indexOf(textToHighlight, ofs)
                if (ofe == -1) break else {
                    // you can change or add more span as per your need
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
                    tv.setText(wordToSpan, TextView.BufferType.SPANNABLE)
                }
                ofs = ofe + 1
            }
        }
    }
}