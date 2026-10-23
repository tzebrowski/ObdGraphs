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

import org.junit.Assert.assertNull
import org.junit.Test
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.PidDefinitionRegistry
import org.obd.metrics.pid.ValueType
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class PidRegistryReloadTest {
    // Both reloads end up preparing the same registry, as when one reads workflow.pidRegistry after the
    // other has already swapped in its own.
    private val registry: PidDefinitionRegistry = registryOf(PIDS)

    private val extraIds = AtomicLong(EXTRA_ID)

    // Like WorkflowOrchestrator: iterate every PID (saved overrides), then register a PID the registry
    // does not have yet (GPS PIDs into a fresh registry), which adds a key mid-iteration of the other thread.
    private val reload =
        PidRegistryReload({ registry }) { r ->
            r.findAll().forEach { it.description = it.description }
            repeat(EXTRAS) { r.register(pid(extraIds.incrementAndGet())) }
        }

    @Test
    fun concurrentReloadsDoNotIterateWhileAnotherRegisters() {
        val failure = AtomicReference<Throwable>()
        val start = CountDownLatch(1)
        val threads =
            (1..2).map {
                thread {
                    start.await()
                    try {
                        repeat(RELOADS) { reload.reload {} }
                    } catch (e: Throwable) {
                        failure.compareAndSet(null, e)
                    }
                }
            }
        start.countDown()
        threads.forEach { it.join() }

        assertNull(failure.get())
    }

    private fun registryOf(count: Int): PidDefinitionRegistry =
        PidDefinitionRegistry.builder().build().apply {
            (1..count).forEach { register(pid(it.toLong())) }
        }

    private fun pid(id: Long) = PidDefinition(id, 2, "A", "22", id.toString(16), "", "pid $id", 0, 100, ValueType.INT)

    private companion object {
        const val PIDS = 2000
        const val EXTRAS = 20
        const val RELOADS = 500
        const val EXTRA_ID = 100_000L
    }
}
