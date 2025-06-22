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

import android.content.Context
import android.content.res.Configuration
import org.obd.graphs.getContext

fun isTablet(): Boolean {
    val context: Context = getContext()!!
    val xlarge =
        context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == 4
    val large =
        context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK ==
                Configuration.SCREENLAYOUT_SIZE_LARGE
    return xlarge || large
}


fun getScreenHeight(): Float {
    val displayMetrics = getContext()!!.resources.displayMetrics
    return displayMetrics.heightPixels / displayMetrics.density
}