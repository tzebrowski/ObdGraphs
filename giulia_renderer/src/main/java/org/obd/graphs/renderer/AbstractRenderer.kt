package org.obd.graphs.renderer

import android.content.Context
import android.graphics.Rect
import org.obd.graphs.bl.collector.CarMetric
import kotlin.math.max

@Suppress("NOTHING_TO_INLINE")
abstract class AbstractRenderer(protected val settings: ScreenSettings, protected val context: Context, protected val fps: Fps)  : ScreenRenderer {

    protected inline fun splitIntoChunks(metrics: List<CarMetric>): MutableList<List<CarMetric>> {
        val lists = metrics.chunked(max(metrics.size / settings.getMaxColumns(), 1)).toMutableList()
        if (lists.size == 3) {
            lists[0] = lists[0]
            lists[1] = lists[1] + lists[2]
            lists.removeAt(2)
        }
        return lists
    }

    protected inline fun initialValueTop(area: Rect): Float =
        when (settings.getMaxColumns()) {
            1 -> area.left + ((area.width()) - 42).toFloat()
            else -> area.left + ((area.width() / 2) - 32).toFloat()
        }
}