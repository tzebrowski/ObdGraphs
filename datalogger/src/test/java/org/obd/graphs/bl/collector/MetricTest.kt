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
package org.obd.graphs.bl.collector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinition

class MetricTest {

    private val pidA = PidDefinition(100L, "010C", "atsh6f1", "RPM", "01", "Codec")

    // Same id as pidA but every other field differs - PidDefinition's equals() is id-only,
    // and so is ObdCommand's (by pid) and Reply's (by command), so this should still collapse
    // into "the same metric" from Metric's point of view.
    private val pidADifferentFields = PidDefinition(100L, "0199", "different query", "different desc", "02", "OtherCodec")

    private val pidB = PidDefinition(200L, "010D", "atsh6f1", "Speed", "01", "Codec")

    private fun obdMetric(pid: PidDefinition, value: Any?): ObdMetric =
        ObdMetric.builder().command(ObdCommand(pid)).value(value).build()

    @Test
    fun `equals and hashCode depend only on the source command pid id`() {
        val m1 = Metric.newInstance(source = obdMetric(pidA, 10), value = 10, min = 0.0, max = 100.0, mean = 50.0)
        val m2 = Metric.newInstance(source = obdMetric(pidADifferentFields, 9999), value = 9999, min = -5.0, max = 5.0, mean = 0.0)

        assertEquals(m1, m2)
        assertEquals(m1.hashCode(), m2.hashCode())
    }

    @Test
    fun `metrics built from different pids are not equal`() {
        val m1 = Metric.newInstance(source = obdMetric(pidA, 1), value = 1)
        val m2 = Metric.newInstance(source = obdMetric(pidB, 1), value = 1)

        assertNotEquals(m1, m2)
    }

    @Test
    fun `a metric is never equal to null or an unrelated type`() {
        val metric = Metric.newInstance(source = obdMetric(pidA, 1), value = 1)

        assertFalse(metric.equals(null))
        assertFalse(metric.equals("not a metric"))
    }

    @Test
    fun `newInstance factory defaults enabled to true and rate to zero`() {
        val metric = Metric.newInstance(source = obdMetric(pidA, 1), value = 1)

        assertTrue(metric.enabled)
        assertEquals(0.0, metric.rate)
    }

    @Test
    fun `newInstance factory defaults min, max and mean to zero when not supplied`() {
        val metric = Metric.newInstance(source = obdMetric(pidA, 1), value = 1)

        assertEquals(0.0, metric.min, 0.0001)
        assertEquals(0.0, metric.max, 0.0001)
        assertEquals(0.0, metric.mean, 0.0001)
    }
}
