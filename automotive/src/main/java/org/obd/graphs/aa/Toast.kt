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
package org.obd.graphs.aa

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.CarToast


internal val toast = Toast()
private const val LOG_TAG = "Toast"

internal class Toast {
    fun show(carCtx: CarContext, id: Int) {
        show(carCtx, carCtx.getString(id))
    }

    fun show(carCtx: CarContext, msg: String) {
        try {
            CarToast.makeText(
                carCtx,
                msg, CarToast.LENGTH_LONG
            ).show()
        } catch (e: Exception){
            Log.w(LOG_TAG,"Failed to show toast",e)
        }
    }
}