 /**
 * Copyright 2019-2026, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.ui.common

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.widget.TextView

fun TextView.setText(
    it: String?,
    color: Int,
    typeface: Int,
    size: Float,
) {
    var valText: String? = it
    if (valText == null) {
        valText = ""
    }

    val valSpanString = SpannableString(valText)
    valSpanString.setSpan(RelativeSizeSpan(size), 0, valSpanString.length, 0) // set size
    valSpanString.setSpan(StyleSpan(typeface), 0, valSpanString.length, 0)
    valSpanString.setSpan(
        ForegroundColorSpan(color),
        0,
        valSpanString.length,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
    text = valSpanString
}

fun TextView.setText(
    it: String?,
    color: Int,
    size: Float,
) {
    setText(it, color, Typeface.BOLD, size)
}

fun TextView.highLightText(
    textToHighlight: String,
    size: Float,
    color: Int,
) {
    val tvt = text.toString()
    var ofe = tvt.indexOf(textToHighlight, 0)
    val wordToSpan: Spannable = SpannableString(text)
    var ofs = 0
    while (ofs < tvt.length && ofe != -1) {
        ofe = tvt.indexOf(textToHighlight, ofs)
        if (ofe == -1) {
            break
        } else {
            wordToSpan.setSpan(
                RelativeSizeSpan(size),
                ofe,
                ofe + textToHighlight.length,
                0,
            ) // set size
            wordToSpan.setSpan(
                ForegroundColorSpan(color),
                ofe,
                ofe + textToHighlight.length,
                0,
            ) // set color
            setText(wordToSpan, TextView.BufferType.SPANNABLE)
        }
        ofs = ofe + 1
    }
}
