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
package org.obd.graphs.bl

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.obd.graphs.bl.collector.InMemoryCarMetricsCollectorTest
import org.obd.graphs.bl.collector.MetricTest
import org.obd.graphs.bl.collector.MetricsBuilderTest
import org.obd.graphs.bl.datalogger.AdjustmentsStrategyTest
import org.obd.graphs.bl.datalogger.AutoConnectTest
import org.obd.graphs.bl.datalogger.MetricsObserverTest
import org.obd.graphs.bl.datalogger.ObdMetricExtTest
import org.obd.graphs.bl.datalogger.PidDefinitionSerializerTest
import org.obd.graphs.bl.datalogger.VehicleCapabilitiesManagerTest
import org.obd.graphs.bl.datalogger.connectors.ConnectionManagerTest
import org.obd.graphs.bl.extra.VehicleStatusMetricsProcessorTest
import org.obd.graphs.bl.gps.GpsMetricsEmitterTest
import org.obd.graphs.bl.query.DragRacingQueryStrategyTest
import org.obd.graphs.bl.query.IndividualQueryStrategyTest
import org.obd.graphs.bl.query.PerformanceQueryStrategyTest
import org.obd.graphs.bl.query.QueryStrategyOrchestratorTest
import org.obd.graphs.bl.query.QueryStrategyTest
import org.obd.graphs.bl.query.SharedQueryStrategyTest
import org.obd.graphs.bl.query.TripInfoQueryStrategyTest
import org.obd.graphs.bl.trip.FileTripRepositoryTest
import org.obd.graphs.bl.trip.TripDescParserTest
import org.obd.graphs.bl.trip.TripModelSerializerTest
import org.obd.graphs.bl.trip.TripVirtualScreenManagerTest

@RunWith(Suite::class)
@SuiteClasses(
    DataLoggerServiceTest::class,
    GpsMetricsEmitterTest::class,
    InMemoryCarMetricsCollectorTest::class,
    MetricTest::class,
    MetricsBuilderTest::class,
    AdjustmentsStrategyTest::class,
    AutoConnectTest::class,
    MetricsObserverTest::class,
    ObdMetricExtTest::class,
    PidDefinitionSerializerTest::class,
    VehicleCapabilitiesManagerTest::class,
    ConnectionManagerTest::class,
    VehicleStatusMetricsProcessorTest::class,
    DragRacingQueryStrategyTest::class,
    IndividualQueryStrategyTest::class,
    PerformanceQueryStrategyTest::class,
    QueryStrategyOrchestratorTest::class,
    QueryStrategyTest::class,
    SharedQueryStrategyTest::class,
    TripInfoQueryStrategyTest::class,
    FileTripRepositoryTest::class,
    TripDescParserTest::class,
    TripModelSerializerTest::class,
    TripVirtualScreenManagerTest::class
)
class DataLoggerTestSuite
