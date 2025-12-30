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
package org.obd.graphs

const val SCREEN_LOCK_PROGRESS_EVENT = "screen.block.event"
const val SCREEN_UNLOCK_PROGRESS_EVENT = "screen.unlock.event"
const val AA_EDIT_PREF_SCREEN = "pref.aa.edit"

const val MAIN_ACTIVITY_EVENT_DESTROYED = "main_activity.on_destroy"
const val MAIN_ACTIVITY_EVENT_PAUSE = "main_activity.on_pause"

const val AA_VIRTUAL_SCREEN_RENDERER_CHANGED_EVENT = "pref.aa.screen_renderer.changed"
const val AA_HIGH_FREQ_PID_SELECTION_CHANGED_EVENT = "pref.pids.generic.high.event.changed"

const val SCREEN_REFRESH_EVENT = "pref.screen_refresh.event"
const val PREF_DYNAMIC_SELECTOR_ENABLED = "pref.aa.theme.dynamic-selector.enabled"
const val PREF_MODULE_LIST = "pref.pids.registry.list"
const val PREF_DRAG_RACE_KEY_PREFIX = "pref.drag_race"

const val PREF_ALERT_LEGEND_ENABLED = "pref.alerting.legend.enabled"
const val PREF_ALERTING_ENABLED = "pref.alerting.enabled"

 const val BACKUP_START = "backup.start"
 const val BACKUP_RESTORE = "backup.restore"
 const val BACKUP_FAILED = "backup.failed"
 const val BACKUP_SUCCESSFUL = "backup.successful"

 const val BACKUP_RESTORE_FAILED = "backup.restore.failed"
 const val BACKUP_RESTORE_SUCCESSFUL = "backup.restore.successful"
 const val BACKUP_RESTORE_NO_FILES = "backup.restore.no_files"

 const val TRIPS_UPLOAD_FAILED = "trips.upload.failed"
 const val TRIPS_UPLOAD_SUCCESSFUL = "trips.upload.successful"
 const val TRIPS_UPLOAD_NO_FILES_SELECTED= "trips.upload.no_files"
 const val GOOGLE_SIGN_IN_GENERAL_FAILURE = "gdrive.authorization.failed"
 const val GOOGLE_SIGN_IN_NO_CREDENTIAL_FAILURE = "gdrive.authorization.no_credentials.failed"
