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
package org.obd.graphs.bl.query

import org.obd.graphs.PREF_DYNAMIC_SELECTOR_ENABLED
import org.obd.graphs.bl.datalogger.PidId
import org.obd.graphs.preferences.Prefs



internal class IndividualQueryStrategy : QueryStrategy() {

    override fun getPIDs(): MutableSet<Long> {
        val pids = super.getPIDs()

        if (Prefs.getBoolean(PREF_DYNAMIC_SELECTOR_ENABLED, false)) {
            pids.add(PidId.EXT_DYNAMIC_SELECTOR_PID_ID.value)
        }

        return pids
    }
}
