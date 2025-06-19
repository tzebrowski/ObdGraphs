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

import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.getContext
import org.obd.metrics.api.model.AdaptiveTimeoutPolicy
import org.obd.metrics.api.model.Adjustments
import org.obd.metrics.api.model.BatchPolicy
import org.obd.metrics.api.model.CachePolicy
import org.obd.metrics.api.model.ErrorsPolicy
import org.obd.metrics.api.model.FormulaExternalParams
import org.obd.metrics.api.model.PidDefinitionCustomization
import org.obd.metrics.api.model.ProducerPolicy
import org.obd.metrics.api.model.STNxxExtensions
import org.obd.metrics.codec.GeneratorPolicy
import java.io.File

internal class AdjustmentsStrategy {

    fun findAdjustmentFor(
        strategy: QueryStrategyType,
        preferences: DataLoggerPreferences = dataLoggerPreferences.instance,
    ): Adjustments =
        when (strategy) {
            QueryStrategyType.DRAG_RACING_QUERY -> getDragRacingAdjustments(preferences = preferences)
            else -> getDefaultAdjustments(preferences = preferences)
        }

    private fun getDragRacingAdjustments(preferences: DataLoggerPreferences): Adjustments {
        var builder =
            Adjustments
                .builder()
                .debugEnabled(preferences.debugLogging)
                .errorsPolicy(
                    ErrorsPolicy
                        .builder()
                        .numberOfRetries(preferences.maxReconnectNum)
                        .reconnectEnabled(preferences.reconnectWhenError)
                        .build(),
                ).batchPolicy(
                    BatchPolicy
                        .builder()
                        .enabled(preferences.batchEnabled)
                        .responseLengthEnabled(preferences.responseLengthEnabled)
                        .mode01BatchSize(preferences.mode01BatchSize)
                        .otherModesBatchSize(preferences.otherModesBatchSize)
                        .build(),
                ).collectRawConnectorResponseEnabled(false)
                .stNxx(
                    STNxxExtensions
                        .builder()
                        .enabled(dataLoggerPreferences.instance.stnExtensionsEnabled)
                        .promoteSlowGroupsEnabled(false)
                        .promoteAllGroupsEnabled(false)
                        .build(),
                ).vehicleMetadataReadingEnabled(false)
                .vehicleCapabilitiesReadingEnabled(false)
                .vehicleDtcReadingEnabled(false)
                .vehicleDtcCleaningEnabled(false)
                .cachePolicy(
                    CachePolicy
                        .builder()
                        .resultCacheEnabled(false)
                        .build(),
                ).producerPolicy(
                    ProducerPolicy
                        .builder()
                        .pidPriority(0, 0) // vehicle speed, rpm
                        .pidPriority(5, 10) // atm pressure, ambient temp
                        .pidPriority(4, 4) // atm pressure, ambient temp
                        .conditionalSleepEnabled(false)
                        .build(),
                ).generatorPolicy(
                    GeneratorPolicy
                        .builder()
                        .enabled(preferences.generatorEnabled)
                        .increment(0.5)
                        .build(),
                ).adaptiveTimeoutPolicy(
                    AdaptiveTimeoutPolicy
                        .builder()
                        .enabled(preferences.adaptiveConnectionEnabled)
                        .checkInterval(5000)
                        .commandFrequency(preferences.dragRacingCommandFrequency)
                        .minimumTimeout(10)
                        .build(),
                )

        if (dataLoggerPreferences.instance.stnExtensionsEnabled) {
            val highPriorityOverridePolicy = PidDefinitionCustomization.builder().priority(0).build()
            builder =
                builder
                    .override(Pid.ATM_PRESSURE_PID_ID.id, highPriorityOverridePolicy)
                    .override(Pid.AMBIENT_TEMP_PID_ID.id, highPriorityOverridePolicy)
                    .override(Pid.DYNAMIC_SELECTOR_PID_ID.id, highPriorityOverridePolicy)
                    .override(Pid.ENGINE_TORQUE_PID_ID.id, PidDefinitionCustomization.builder().priority(4).build())
        }

        return builder.build()
    }

    private fun getDefaultAdjustments(preferences: DataLoggerPreferences) =
        Adjustments
            .builder()
            .debugEnabled(preferences.debugLogging)
            .override(Pid.DISTANCE_PID_ID.id, PidDefinitionCustomization.builder().lastInTheQuery(true).build())
            .formulaExternalParams(FormulaExternalParams.builder().param("unit_tank_size", preferences.fuelTankSize).build())
            .errorsPolicy(
                ErrorsPolicy
                    .builder()
                    .numberOfRetries(preferences.maxReconnectNum)
                    .reconnectEnabled(preferences.reconnectWhenError)
                    .build(),
            ).batchPolicy(
                BatchPolicy
                    .builder()
                    .enabled(preferences.batchEnabled)
                    .strictValidationEnabled(preferences.batchStricValidationEnabled)
                    .responseLengthEnabled(preferences.responseLengthEnabled)
                    .mode01BatchSize(preferences.mode01BatchSize)
                    .otherModesBatchSize(preferences.otherModesBatchSize)
                    .build(),
            ).collectRawConnectorResponseEnabled(preferences.dumpRawConnectorResponse)
            .stNxx(
                STNxxExtensions
                    .builder()
                    .promoteSlowGroupsEnabled(preferences.stnExtensionsEnabled)
                    .promoteAllGroupsEnabled(preferences.stnExtensionsEnabled)
                    .enabled(preferences.stnExtensionsEnabled)
                    .build(),
            ).vehicleMetadataReadingEnabled(preferences.vehicleMetadataReadingEnabled)
            .vehicleCapabilitiesReadingEnabled(preferences.vehicleCapabilitiesReadingEnabled)
            .vehicleDtcReadingEnabled(preferences.vehicleDTCReadingEnabled)
            .vehicleDtcCleaningEnabled(preferences.vehicleDTCCleaningEnabled)
            .cachePolicy(
                CachePolicy
                    .builder()
                    .resultCacheFilePath(File(getContext()?.cacheDir, "formula_cache.json").absolutePath)
                    .resultCacheEnabled(preferences.resultsCacheEnabled)
                    .build(),
            ).producerPolicy(
                ProducerPolicy
                    .builder()
                    .conditionalSleepEnabled(preferences.adaptiveConnectionEnabled)
                    .conditionalSleepSliceSize(10)
                    .build(),
            ).generatorPolicy(
                GeneratorPolicy
                    .builder()
                    .enabled(preferences.generatorEnabled)
                    .increment(0.5)
                    .build(),
            ).adaptiveTimeoutPolicy(
                AdaptiveTimeoutPolicy
                    .builder()
                    .enabled(preferences.adaptiveConnectionEnabled)
                    .checkInterval(5000)
                    .commandFrequency(preferences.commandFrequency)
                    .minimumTimeout(10)
                    .build(),
            ).build()
}
