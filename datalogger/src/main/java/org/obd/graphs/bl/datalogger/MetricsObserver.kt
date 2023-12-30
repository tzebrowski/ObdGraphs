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
package org.obd.graphs.bl.datalogger

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import org.obd.graphs.MetricsProcessor
import org.obd.metrics.api.model.*

internal class MetricsObserver : Lifecycle, ReplyObserver<Reply<*>>() {

    private val metrics: MutableLiveData<ObdMetric> = MutableLiveData<ObdMetric>()
    private val metricsProcessors = mutableSetOf<MetricsProcessor>()

    override fun onStopped() {
        metrics.postValue(null)
        metricsProcessors.forEach { it.onStopped() }
    }

    override fun onRunning(vehicleCapabilities: VehicleCapabilities?) {
        metricsProcessors.forEach { it.onRunning(vehicleCapabilities) }
    }

    fun observe(metricsProcessor: MetricsProcessor) {
        metricsProcessors.add(metricsProcessor)
    }

    fun observe(lifecycleOwner: LifecycleOwner, observer: (metric: ObdMetric) -> Unit) {
        metrics.observe(lifecycleOwner) {
            it?.let {
                observer(it)
            }
        }
    }

    override fun onNext(reply: Reply<*>) {

        if (reply is ObdMetric) {
            reply.command.pid?.let {
                metrics.postValue(reply)
                metricsProcessors.forEach { it.postValue(reply) }
            }
        }
    }
}