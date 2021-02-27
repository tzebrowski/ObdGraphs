package org.openobd2.core.logger.ui.common

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan

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

    }

}