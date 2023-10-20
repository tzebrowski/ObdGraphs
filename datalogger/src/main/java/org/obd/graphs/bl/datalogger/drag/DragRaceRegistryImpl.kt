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
package org.obd.graphs.bl.datalogger.drag

import android.util.Log

private const val LOG_KEY = "DragRaceRegistry"

internal class DragRaceRegistryImpl : DragRaceRegistry {

    private val results = DragRaceResults()

    override fun update0100(value: Long) {
        if (value == 0L) {
            Log.v(LOG_KEY, "Invalid value")
        } else {
            results.last._0_100val = if (results.last._0_100val == VALUE_NOT_SET) {
                value
            } else {
                results.current._0_100val
            }

            results.current._0_100val = value

            if (results.best._0_100val > value || results.best._0_100val == VALUE_NOT_SET) {
                results.best._0_100val = value
            }
        }
    }

    override fun update0160(value: Long) {
        if (value == 0L) {
            Log.v(LOG_KEY, "Invalid value")
        } else {
            results.last._0_160val = if (results.last._0_160val == VALUE_NOT_SET) {
                value
            } else {
                results.current._0_160val
            }

            results.current._0_160val = value

            if (results.best._0_160val > value || results.best._0_160val == VALUE_NOT_SET) {
                results.best._0_160val = value
            }
        }
    }

    override fun update100200(value: Long) {
        if (value == 0L) {
            Log.v(LOG_KEY, "Invalid value")
        } else {

            results.last._100_200val = if (results.last._100_200val == VALUE_NOT_SET) {
                value
            } else {
                results.current._100_200val
            }

            results.current._100_200val = value

            if (results.best._100_200val > value || results.best._100_200val == VALUE_NOT_SET) {
                results.best._100_200val = value
            }
        }
    }

    override fun getResult(): DragRaceResults = results
}