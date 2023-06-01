package org.obd.graphs.ui.common

import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.widget.Toast
import org.obd.graphs.getContext

fun toast(id: Int,vararg formatArgs: String) {
    getContext()?.let {
        val text = it.resources.getString(id, *formatArgs)
        val biggerText = SpannableStringBuilder(text)
        biggerText.setSpan(RelativeSizeSpan(1.0f), 0, text.length, 0)
        Toast.makeText(
            it, biggerText,
            Toast.LENGTH_LONG
        ).run {
            show()
        }
    }
}