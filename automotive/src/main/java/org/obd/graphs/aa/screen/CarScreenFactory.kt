package org.obd.graphs.aa.screen

import androidx.car.app.CarContext
import org.obd.graphs.aa.CarSettings
import org.obd.graphs.aa.ScreenTemplateType
import org.obd.graphs.aa.screen.iot.IotTemplateCarScreen
import org.obd.graphs.aa.screen.nav.NavTemplateCarScreen
import org.obd.graphs.bl.collector.CarMetricsCollector
import org.obd.graphs.renderer.Fps

internal interface CarScreenFactory {

    companion object {
        fun instance(
            carContext: CarContext,
            settings: CarSettings,
            metricsCollector: CarMetricsCollector,
            fps: Fps
        ): CarScreen =
            if (settings.getScreenTemplate() == ScreenTemplateType.NAV) {
                NavTemplateCarScreen(carContext, settings, metricsCollector, fps)
            } else {
                IotTemplateCarScreen(carContext, settings, metricsCollector)
            }
    }
}