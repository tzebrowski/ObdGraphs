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

const val WORKFLOW_RELOAD_EVENT = "data.logger.workflow.reload.event"
const val RESOURCE_LIST_CHANGED_EVENT = "data.logger.resources.changed.event"
const val PROFILE_CHANGED_EVENT = "data.logger.profile.changed.event"
const val DATA_LOGGER_ADAPTER_NOT_SET_EVENT = "data.logger.adapter.not_set"
const val DATA_LOGGER_ERROR_CONNECT_EVENT = "data.logger.error.connect"
const val DATA_LOGGER_WIFI_INCORRECT = "data.logger.error.wifi.incorrect"
const val DATA_LOGGER_WIFI_NOT_CONNECTED = "data.logger.error.wifi.not.connected"
const val DATA_LOGGER_CONNECTED_EVENT = "data.logger.connected"
const val DATA_LOGGER_DTC_AVAILABLE = "data.logger.dtc.available"
const val DATA_LOGGER_CONNECTING_EVENT = "data.logger.connecting"
const val DATA_LOGGER_STOPPED_EVENT = "data.logger.stopped"
const val DATA_LOGGER_STOPPING_EVENT = "data.logger.stopping"
const val DATA_LOGGER_ERROR_EVENT = "data.logger.error"
const val DATA_LOGGER_NO_NETWORK_EVENT = "data.logger.network_error"


const val EVENT_DYNAMIC_SELECTOR_MODE_NORMAL = "event.dynamic.selector.mode.normal"
const val EVENT_DYNAMIC_SELECTOR_MODE_ECO = "event.dynamic.selector.mode.eco"
const val EVENT_DYNAMIC_SELECTOR_MODE_SPORT = "event.dynamic.selector.mode.sport"
const val EVENT_DYNAMIC_SELECTOR_MODE_RACE = "event.dynamic.selector.mode.race"

internal const val LOGGER_TAG = "DataLogger"