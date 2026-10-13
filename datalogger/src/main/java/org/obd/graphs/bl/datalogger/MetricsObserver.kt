/*
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

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import org.obd.metrics.api.model.*
import java.util.concurrent.CopyOnWriteArraySet

private const val TAG = "MetricsObserver"

internal class MetricsObserver : Lifecycle, ReplyObserver<Reply<*>>() {

    private val metrics: MutableLiveData<ObdMetric> = MutableLiveData<ObdMetric>()
    private val metricsProcessors = mutableSetOf<MetricsProcessor>()
    private val eachMetricObservers = CopyOnWriteArraySet<(ObdMetric) -> Unit>()
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    override fun onStopped() {
        metrics.postValue(null)
        metricsProcessors.forEach { it.onStopped() }
    }

    override fun onRunning(vehicleCapabilities: VehicleCapabilities?) {
        metricsProcessors.forEach { it.onRunning(vehicleCapabilities) }
    }

    fun observe(metricsProcessor: MetricsProcessor) {
        metricsProcessors.add(metricsProcessor)
        metricsProcessor.init(this)
    }

    fun observe(lifecycleOwner: LifecycleOwner, observer: (metric: ObdMetric) -> Unit) {
        metrics.observe(lifecycleOwner) {
            it?.let {
                observer(it)
            }
        }
    }

    // Every metric, in order, on the main thread, until the owner is destroyed. observe() goes through LiveData's
    // postValue, which keeps only the latest value: of two metrics decoded from one batch reply, the first was lost
    // (the readiness dialog never saw 01 01, posted right before 01 41). Use this when each PID's value matters.
    fun observeEach(lifecycleOwner: LifecycleOwner, observer: (metric: ObdMetric) -> Unit) {
        if (lifecycleOwner.lifecycle.currentState == State.DESTROYED) return
        eachMetricObservers.add(observer)
        lifecycleOwner.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Event.ON_DESTROY) eachMetricObservers.remove(observer)
            }
        )
    }

    override fun onNext(reply: Reply<*>) {

        if (reply is ObdMetric) {
            reply.command.pid?.let {
                metrics.postValue(reply)
                eachMetricObservers.forEach { observer ->
                    mainHandler.post { if (observer in eachMetricObservers) observer(reply) }
                }
                metricsProcessors.forEach {
                    try {
                        it.postValue(reply)
                    } catch (e: java.lang.Exception) {
                        if (Log.isLoggable(TAG,Log.VERBOSE)){
                            Log.v(TAG,"Failed to process metric",e)
                        }
                    }
                }
            }
        }
    }
}
