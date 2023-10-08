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
package org.obd.graphs.profile

import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getString

const val PROFILE_ID_PREF = "pref.profile.id"
const val PROFILE_NAME_PREFIX = "pref.profile.names"
private const val DEFAULT_MAX_PROFILES = 12

fun getProfiles() =
    (1..DEFAULT_MAX_PROFILES)
        .associate {
            "profile_$it" to Prefs.getString(
                "$PROFILE_NAME_PREFIX.profile_$it",
                "Profile $it"
            )
        }


fun getSelectedProfile(): String = Prefs.getString(PROFILE_ID_PREF)!!

fun getSelectedProfileName(): String? = Prefs.getString("$PROFILE_NAME_PREFIX.${getSelectedProfile()}", "")