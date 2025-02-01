 /**
 * Copyright 2019-2025, Tomasz Żebrowski
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

import android.app.Activity
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.dataLogger
import org.obd.graphs.bl.query.Query

private const val LOG_TAG = "FloatingButton"

fun attachToFloatingButton(
    activity: Activity?,
    query: Query,
) {
    activity?.let {
        val btn = activity.findViewById<FloatingActionButton>(R.id.connect_btn)
        btn?.setOnClickListener {
            if (dataLogger.isRunning()) {
                Log.i(org.obd.graphs.activity.LOG_TAG, "DragRacingFragment: Start data logging")
                dataLogger.stop()
            } else {
                Log.i(LOG_TAG, "Start data logging")
                dataLogger.start(query)
            }
        }

        btn?.backgroundTintList =
            ContextCompat.getColorStateList(activity, if (dataLogger.isRunning()) org.obd.graphs.commons.R.color.cardinal
            else org.obd.graphs.commons.R.color.philippine_green)
    }
}
