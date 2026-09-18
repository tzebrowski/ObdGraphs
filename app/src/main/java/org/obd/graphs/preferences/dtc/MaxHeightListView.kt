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
import android.widget.ListView

// A ListView that never grows beyond a fraction of the screen height. Inside the DTC module
// picker (an AlertDialog custom view) a plain wrap_content/weighted ListView still claimed the
// whole dialog height when the module list was long, pushing the button bar out of view - an
// explicit cap keeps room for the buttons regardless of how the dialog window gets measured.
class MaxHeightListView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.listViewStyle
) : ListView(context, attrs, defStyleAttr) {
    var maxHeightRatio: Float = 0.4f

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        val maxHeight = (resources.displayMetrics.heightPixels * maxHeightRatio).toInt()
        val size = MeasureSpec.getSize(heightMeasureSpec)
        val cappedSpec =
            when (MeasureSpec.getMode(heightMeasureSpec)) {
                MeasureSpec.UNSPECIFIED -> MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
                MeasureSpec.EXACTLY -> MeasureSpec.makeMeasureSpec(minOf(size, maxHeight), MeasureSpec.EXACTLY)
                else -> MeasureSpec.makeMeasureSpec(minOf(size, maxHeight), MeasureSpec.AT_MOST)
            }
        super.onMeasure(widthMeasureSpec, cappedSpec)
    }
}
