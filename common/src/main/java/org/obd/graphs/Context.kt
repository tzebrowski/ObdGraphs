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
package org.obd.graphs

import android.content.ContextWrapper
import java.lang.ref.WeakReference

private lateinit var activityContext: WeakReference<ContextWrapper>
private lateinit var carContext: WeakReference<ContextWrapper>

fun setActivityContext(activity: ContextWrapper) {
    activityContext = WeakReference(activity)
}

fun setCarContext(carContext: ContextWrapper) {
    org.obd.graphs.carContext = WeakReference(carContext)
}

fun getContext(): ContextWrapper? =
    when {
        //Application context has priority over Car context
        ::activityContext.isInitialized -> {
            activityContext.get()
        }
        ::carContext.isInitialized -> {
            carContext.get()
        }
        else -> {
            null
        }
    }