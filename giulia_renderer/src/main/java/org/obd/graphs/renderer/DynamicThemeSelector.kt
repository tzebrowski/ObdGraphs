package org.obd.graphs.renderer

import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_ECO
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_NORMAL
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_RACE
import org.obd.graphs.ui.common.COLOR_DYNAMIC_SELECTOR_SPORT

private const val DYNAMIC_SELECTOR_PID_ID = 7036L

internal class DynamicThemeSelector(private val settings: ScreenSettings, private val metricsCollector: CarMetricsCollector) {

    fun updateTheme() {

        if (settings.isDynamicSelectorThemeEnabled()) {
            // dynamic selector
            findDynamicSelectorPID()?.let {
                when (it.value.toInt()) {
                    0 ->
                        //normal blue
                        settings.colorTheme().progressColor = COLOR_DYNAMIC_SELECTOR_NORMAL

                    2 ->
                        //sport red
                        settings.colorTheme().progressColor = COLOR_DYNAMIC_SELECTOR_SPORT

                    4 ->
                        //eco
                        settings.colorTheme().progressColor = COLOR_DYNAMIC_SELECTOR_ECO

                    else -> {
                        //race yellow
                        settings.colorTheme().progressColor = COLOR_DYNAMIC_SELECTOR_RACE
                    }
                }
            }
        }
    }

    private fun findDynamicSelectorPID() =
        metricsCollector.metricBy(DYNAMIC_SELECTOR_PID_ID)
}