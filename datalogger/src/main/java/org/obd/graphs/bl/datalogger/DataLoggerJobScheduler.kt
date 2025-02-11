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
package org.obd.graphs.bl.datalogger

import android.util.Log
import org.obd.graphs.bl.query.Query
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

internal class DataLoggerJobScheduler {

    private val scheduleService: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private var future: ScheduledFuture<*>? = null

    fun stop() {
        Log.i(
            LOG_TAG,
            "Canceling data logger scheduled task"
        )

        future?.cancel(true)
    }

    fun schedule(delay: Long, query: Query) {
        val task = Runnable {
            Log.i(LOG_TAG, "Starting data logger task for = ${query.getIDs()} .............")
            dataLogger.start(query)
        }
        Log.i(
            LOG_TAG,
            "Scheduled data logger task with the delay=${delay}s"
        )
        future?.cancel(true)

        future = scheduleService.schedule(
            task,
            delay,
            TimeUnit.SECONDS
        )
    }
}
