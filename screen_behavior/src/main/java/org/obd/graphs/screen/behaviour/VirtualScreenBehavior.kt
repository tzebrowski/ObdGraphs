package org.obd.graphs.screen.behaviour

import android.content.Context
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.bl.query.QueryStrategyType
import org.obd.graphs.renderer.api.Fps
import org.obd.graphs.renderer.api.ScreenSettings
import org.obd.graphs.renderer.api.SurfaceRendererType
import org.obd.graphs.renderer.api.VirtualScreenConfig

abstract class VirtualScreenBehavior(
    context: Context,
    metricsCollector: MetricsCollector,
    settings: ScreenSettings,
    fps: Fps,
    surfaceRendererType: SurfaceRendererType
) : ScreenBehavior(context, metricsCollector, settings, fps, surfaceRendererType) {

    protected abstract val virtualScreenConfig: VirtualScreenConfig

    override fun getCurrentVirtualScreen(): Int = virtualScreenConfig.getVirtualScreen()
    override fun setCurrentVirtualScreen(id: Int) = virtualScreenConfig.setVirtualScreen(id)
    override fun getSelectedPIDs(): Set<Long> = virtualScreenConfig.selectedPIDs
    override fun getSortOrder(): Map<Long, Int>? = virtualScreenConfig.getPIDsSortOrder()

    override fun applyFilters(metricsCollector: MetricsCollector) {
        query.setStrategy(queryStrategyType())
        val selectedPIDs = getSelectedPIDs()
        val sortOrder = getSortOrder()

        when (queryStrategyType()) {
            QueryStrategyType.INDIVIDUAL_QUERY -> {
                metricsCollector.applyFilter(enabled = selectedPIDs, order = sortOrder)
                query.update(metricsCollector.getMetrics().map { p -> p.source.command.pid.id }.toSet())
            }

            QueryStrategyType.SHARED_QUERY -> {
                val queryIds = query.getIDs()
                val intersection = selectedPIDs.filter { queryIds.contains(it) }.toSet()
                metricsCollector.applyFilter(enabled = intersection, order = sortOrder)
            }

            else -> {}
        }
    }
}