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
package org.obd.graphs.aa

import android.graphics.Color
import androidx.car.app.model.CarColor
import org.obd.graphs.ui.common.COLOR_WHITE

internal fun mapColor(color: Int): CarColor {
    return when (color) {
        -1 -> CarColor.PRIMARY
        -48060 -> CarColor.RED
        -6697984 -> CarColor.GREEN
        -17613 -> CarColor.YELLOW
        -16737844 -> CarColor.BLUE
        -4053450 -> CarColor.RED

        COLOR_WHITE -> CarColor.PRIMARY
        Color.RED -> CarColor.RED
        Color.BLUE -> CarColor.BLUE
        Color.GREEN -> CarColor.GREEN
        Color.YELLOW -> CarColor.YELLOW
        else -> CarColor.PRIMARY
    }
}
