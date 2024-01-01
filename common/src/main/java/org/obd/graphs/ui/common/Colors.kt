/**
 * Copyright 2019-2023, Tomasz Żebrowski
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.obd.graphs.ui.common

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.core.content.ContextCompat
import org.obd.graphs.commons.R
import org.obd.graphs.getContext

fun String.colorize(color: Int, typeface: Int,start: Int, end: Int, size: Float) : SpannableString {

    var valText: String? = this
    if (valText == null) {
        valText = ""
    }

    val valSpanString = SpannableString(valText)
    valSpanString.setSpan(RelativeSizeSpan(size), 0, valSpanString.length, 0) // set size
    valSpanString.setSpan(StyleSpan(typeface), 0, valSpanString.length, 0)
    if (valSpanString.length >= end) {
        valSpanString.setSpan(
            ForegroundColorSpan(color),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    return valSpanString
}

fun String.colorize(color: Int, typeface: Int, size: Float) : SpannableString {

    var valText: String? = this
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
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    return valSpanString
}

class Colors {

    private val base: List<Int> =  mutableListOf<Int>().apply {
        add(COLOR_CARDINAL)
        add(COLOR_PHILIPPINE_GREEN)
        add(Color.parseColor("#1C3D72"))
        add(Color.parseColor("#BBBBBB"))

        add(Color.parseColor("#C0CA33"))
        add(Color.parseColor("#FF9800"))
        add(Color.parseColor("#F44336"))
        add(Color.parseColor("#4A148C"))
        add(Color.parseColor("#FFFF00"))
        add(Color.parseColor("#42A5F5"))
        add(Color.parseColor("#4DB6AC"))
        add(Color.parseColor("#3F51B5"))

        add(Color.parseColor("#FF6F00"))
        add(Color.parseColor("#E8F5E9"))
        add(Color.parseColor("#757575"))
        add(Color.parseColor("#FFCCBC"))
        add(Color.parseColor("#00C853"))
        add(Color.parseColor("#66BB6A"))
    }

    fun generate(): IntIterator {
        return base.toIntArray().iterator()
    }
}

val COLOR_CARDINAL: Int = color(R.color.cardinal)
val COLOR_PHILIPPINE_GREEN: Int = color(R.color.philippine_green)
val COLOR_RAINBOW_INDIGO: Int = color(R.color.rainbow_indigo)
val COLOR_LIGHT_SHADE_GRAY: Int = color(R.color.light_shade_gray)
val COLOR_TRANSPARENT: Int = color(R.color.transparent)

val COLOR_DYNAMIC_SELECTOR_RACE: Int = color(R.color.dynamic_selector_race)
val COLOR_DYNAMIC_SELECTOR_ECO: Int = color(R.color.dynamic_selector_eco)
val COLOR_DYNAMIC_SELECTOR_SPORT: Int = color(R.color.dynamic_selector_sport)
val COLOR_DYNAMIC_SELECTOR_NORMAL: Int = color(R.color.dynamic_selector_normal)

val COLOR_WHITE: Int  = color(R.color.white)

fun color(id: Int) = ContextCompat.getColor(getContext()!!, id)


