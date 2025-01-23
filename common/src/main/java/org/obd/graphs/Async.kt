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
@file:Suppress("DEPRECATION", "UNCHECKED_CAST")

package org.obd.graphs

import android.os.AsyncTask


fun <T> runAsync(wait: Boolean = true, handler: () -> T) : T {
    val asyncJob: AsyncTask<Void, Void, T> = object : AsyncTask<Void, Void, T>() {
        @Deprecated("Deprecated in Java", ReplaceWith("handler()"))
        override fun doInBackground(vararg params: Void?): T {
            return handler()
        }
    }
    return if (wait) {
        asyncJob.execute().get()
    } else {
        asyncJob.execute()
        null as T
    }
}