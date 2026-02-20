 /**
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
package org.obd.graphs.ui.performance

import org.obd.graphs.R
import org.obd.graphs.bl.query.Query
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.renderer.api.ScreenSettings
import org.obd.graphs.renderer.api.SurfaceRendererType
import org.obd.graphs.ui.SurfaceRendererFragment

internal class PerformanceRendererFragment : SurfaceRendererFragment(
    R.layout.fragment_performance,
    SurfaceRendererType.PERFORMANCE) {

    private val query: Query = Query.instance(QueryStrategyType.PERFORMANCE_QUERY)
    private val settings = PerformanceSettings()

    override fun query(): Query  = query
    override fun getScreenSettings(): ScreenSettings = settings
}
