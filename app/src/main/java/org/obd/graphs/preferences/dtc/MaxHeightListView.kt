/*
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
package org.obd.graphs.preferences.dtc

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import org.obd.graphs.R

// A ListView that never grows beyond a fraction of the screen height. Inside the DTC module
// picker (an AlertDialog custom view) a plain wrap_content/weighted ListView still claimed the
// whole dialog height when the module list was long, pushing the button bar out of view - an
// explicit cap keeps room for the buttons regardless of how the dialog window gets measured.
// The cap also leaves room for the views around the list: with the Clear flow's warning and the
// UDS / OBD-II hint above it, a fixed 40 % pushed the buttons off a phone screen.
class MaxHeightListView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.listViewStyle
) : ListView(context, attrs, defStyleAttr) {
    var maxHeightRatio: Float = resources.getInteger(R.integer.dtc_module_list_max_height_percent) / 100f

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        val maxHeight =
            pickerListMaxHeight(
                screenHeight = resources.displayMetrics.heightPixels,
                ratio = maxHeightRatio,
                reserved = siblingsHeight(MeasureSpec.getSize(widthMeasureSpec)),
                minHeight = (MIN_HEIGHT_DP * resources.displayMetrics.density).toInt()
            )
        val size = MeasureSpec.getSize(heightMeasureSpec)
        val cappedSpec =
            when (MeasureSpec.getMode(heightMeasureSpec)) {
                MeasureSpec.UNSPECIFIED -> MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
                MeasureSpec.EXACTLY -> MeasureSpec.makeMeasureSpec(minOf(size, maxHeight), MeasureSpec.EXACTLY)
                else -> MeasureSpec.makeMeasureSpec(minOf(size, maxHeight), MeasureSpec.AT_MOST)
            }
        super.onMeasure(widthMeasureSpec, cappedSpec)
    }

    // Height of everything else in the parent (the picker's texts, buttons and padding), measured at the list's width.
    private fun siblingsHeight(width: Int): Int {
        val parent = parent as? ViewGroup ?: return 0
        val widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST)
        val heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        var height = parent.paddingTop + parent.paddingBottom
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child === this || child.visibility == View.GONE) continue
            child.measure(widthSpec, heightSpec)
            val margins = child.layoutParams as? ViewGroup.MarginLayoutParams
            height += child.measuredHeight + (margins?.topMargin ?: 0) + (margins?.bottomMargin ?: 0)
        }
        return height
    }

    private companion object {
        // About two rows: the list never disappears entirely, even on a very small screen.
        const val MIN_HEIGHT_DP = 96
    }
}

// The dialog window gets about this share of the screen; the rest is its margins and title.
internal const val PICKER_DIALOG_SCREEN_SHARE = 0.8f

// The list's height cap: its share of the screen, but never more than the dialog has left once the views around it
// (reserved) are placed; minHeight wins over both.
internal fun pickerListMaxHeight(
    screenHeight: Int,
    ratio: Float,
    reserved: Int,
    minHeight: Int
): Int {
    val left = (screenHeight * PICKER_DIALOG_SCREEN_SHARE).toInt() - reserved
    return maxOf(minHeight, minOf((screenHeight * ratio).toInt(), left))
}
