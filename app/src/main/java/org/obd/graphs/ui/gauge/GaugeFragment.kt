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
package org.obd.graphs.ui.gauge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import org.obd.graphs.R
import org.obd.graphs.bl.datalogger.DataLoggerRepository
import org.obd.graphs.bl.query.Query
import org.obd.graphs.renderer.ScreenSettings
import org.obd.graphs.renderer.SurfaceRendererType
import org.obd.graphs.ui.SurfaceFragment
import org.obd.graphs.ui.common.COLOR_PHILIPPINE_GREEN
import org.obd.graphs.ui.common.COLOR_TRANSPARENT
import org.obd.graphs.ui.withDataLogger

 internal class GaugeFragment : SurfaceFragment(
    R.layout.fragment_gauge,
    SurfaceRendererType.GAUGE,
) {

    private val query: Query = Query.instance()
    private val settings = GaugeSettings(query)

    override fun getScreenSettings(): ScreenSettings = settings
    override fun query() = query.apply(gaugeVirtualScreenPreferences.getVirtualScreenPrefKey())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        setupVirtualViewPanel()
        return view
    }

    private fun setVirtualViewBtn(
        btnId: Int,
        selection: String,
        viewId: String,
    ) {
        (root.findViewById<Button>(btnId)).let {
            if (selection == viewId) {
                it.setBackgroundColor(COLOR_PHILIPPINE_GREEN)
            } else {
                it.setBackgroundColor(COLOR_TRANSPARENT)
            }

            it.setOnClickListener {
                gaugeVirtualScreenPreferences.updateVirtualScreen(viewId)

                if (DataLoggerRepository.isRunning()) {
                    withDataLogger {
                        updateQuery(query())
                    }
                }

                applyFilter()
                setupVirtualViewPanel()
                surfaceController.renderFrame()
            }
        }
    }

     private fun applyFilter() = metricsCollector.applyFilter(query.filterBy(gaugeVirtualScreenPreferences.getVirtualScreenPrefKey()))


     private fun setupVirtualViewPanel() {
        val currentVirtualScreen = gaugeVirtualScreenPreferences.getCurrentVirtualScreen()
        setVirtualViewBtn(R.id.virtual_view_1, currentVirtualScreen, "1")
        setVirtualViewBtn(R.id.virtual_view_2, currentVirtualScreen, "2")
        setVirtualViewBtn(R.id.virtual_view_3, currentVirtualScreen, "3")
        setVirtualViewBtn(R.id.virtual_view_4, currentVirtualScreen, "4")
        setVirtualViewBtn(R.id.virtual_view_5, currentVirtualScreen, "5")
        setVirtualViewBtn(R.id.virtual_view_6, currentVirtualScreen, "6")
        setVirtualViewBtn(R.id.virtual_view_7, currentVirtualScreen, "7")
        setVirtualViewBtn(R.id.virtual_view_8, currentVirtualScreen, "8")
    }
}
