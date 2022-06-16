package org.obd.graphs.ui.common

import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.widget.Toast
import org.obd.graphs.ApplicationContext

fun toast(id: Int) {
    ApplicationContext.get()?.let {
        val text = it.resources.getString(id)
        val biggerText = SpannableStringBuilder(text)
        biggerText.setSpan(RelativeSizeSpan(1.25f), 0, text.length, 0)
        Toast.makeText(
            it, biggerText,
            Toast.LENGTH_LONG
        ).run {
            setGravity(Gravity.CENTER, 0, 0)
            show()
        }
    }
}