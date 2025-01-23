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

import android.content.BroadcastReceiver
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.bl.query.Query
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.diagnostic.Diagnostics
import org.obd.metrics.diagnostic.Histogram
import org.obd.metrics.diagnostic.Rate
import org.obd.metrics.pid.PidDefinitionRegistry
import java.util.*

interface DataLogger {
    val eventsReceiver: BroadcastReceiver
    fun status(): WorkflowStatus
    fun observe(lifecycleOwner: LifecycleOwner, observer: (metric: ObdMetric) -> Unit)
    fun observe(metricsProcessor: MetricsProcessor): DataLogger
    fun isRunning(): Boolean
    fun getDiagnostics(): Diagnostics
    fun findHistogramFor(metric: ObdMetric): Histogram
    fun findRateFor(metric: ObdMetric): Optional<Rate>
    fun getPidDefinitionRegistry(): PidDefinitionRegistry
    fun isDTCEnabled(): Boolean
    fun scheduleStart(delay: Long, query: Query)
    fun scheduledStop()
    fun executeRoutine(query: Query)
    fun start(query: Query)
    fun updateQuery(query: Query)
    fun stop()
    fun getCurrentQuery(): Query?
}