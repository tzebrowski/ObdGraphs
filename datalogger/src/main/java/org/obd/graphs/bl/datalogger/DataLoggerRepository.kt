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
package org.obd.graphs.bl.datalogger

import android.content.BroadcastReceiver
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import org.obd.graphs.bl.query.Query
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.diagnostic.Diagnostics
import org.obd.metrics.diagnostic.Histogram
import org.obd.metrics.diagnostic.Rate
import org.obd.metrics.pid.PidDefinitionRegistry
import java.util.Optional

object DataLoggerRepository {
    private var _workflowOrchestrator: WorkflowOrchestrator? = null

    internal val workflowOrchestrator: WorkflowOrchestrator
        get() {
            if (_workflowOrchestrator == null) {
                Log.i(LOG_TAG, "Initializing WorkflowOrchestrator")
                _workflowOrchestrator = WorkflowOrchestrator()
            }
            return _workflowOrchestrator!!
        }

    @VisibleForTesting
    internal fun setWorkflowOrchestrator(mock: WorkflowOrchestrator) {
        _workflowOrchestrator = mock
    }

    fun getCurrentQuery(): Query? = workflowOrchestrator.getCurrentQuery()
    fun isRunning(): Boolean = workflowOrchestrator.isRunning()
    fun getDiagnostics(): Diagnostics = workflowOrchestrator.diagnostics()
    fun findHistogramFor(metric: ObdMetric): Histogram = workflowOrchestrator.findHistogramFor(metric)
    fun findRateFor(metric: ObdMetric): Optional<Rate> = workflowOrchestrator.findRateFor(metric)
    fun getPidDefinitionRegistry(): PidDefinitionRegistry = workflowOrchestrator.pidDefinitionRegistry()
    fun isDTCEnabled(): Boolean = workflowOrchestrator.isDTCEnabled()
    fun status(): WorkflowStatus = workflowOrchestrator.status()

    fun observe(lifecycleOwner: LifecycleOwner, observer: (metric: ObdMetric) -> Unit) {
        workflowOrchestrator.observe(lifecycleOwner, observer)
    }

    fun observe(metricsProcessor: MetricsProcessor): DataLoggerRepository {
        workflowOrchestrator.observe(metricsProcessor)
        return this
    }


    val eventsReceiver: BroadcastReceiver
        get() = workflowOrchestrator.eventsReceiver
}
