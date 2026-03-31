/*
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
package org.obd.graphs.renderer.api

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import org.obd.graphs.bl.collector.MetricsCollector
import org.obd.graphs.renderer.drag.DragRacingSurfaceRenderer
import org.obd.graphs.renderer.gauge.GaugeSurfaceRenderer
import org.obd.graphs.renderer.giulia.GiuliaSurfaceRenderer
import org.obd.graphs.renderer.performance.PerformanceSurfaceRenderer
import org.obd.graphs.renderer.trip.TripInfoSurfaceRenderer

/**
 * Defines the contract for rendering telemetry data and graphical UI components onto an Android [Canvas].
 * * Implementations of this interface are responsible for specific visualization styles,
 * such as gauges, performance metrics, or trip summaries.
 */
interface SurfaceRenderer {

    /**
     * Renders the visual content of the renderer onto the provided [canvas].
     *
     * @param canvas The target [Canvas] for drawing operations.
     * @param drawArea The specific [Rect] boundaries for rendering. If the provided area is null or empty,
     * the implementation typically defaults to the full dimensions of the canvas.
     */
    fun onDraw(canvas: Canvas, drawArea: Rect?)

    /**
     * Performs a "hard teardown" by releasing heavy resources to prevent memory leaks.
     * * This should be called when the renderer is being discarded or replaced. It is responsible for
     * cleaning up resources like Bitmaps, unregistering listeners, or clearing large data structures.
     */
    fun recycle()

    /**
     * Updates the vertical scroll position for content that exceeds the viewport height.
     *
     * @param scrollOffset The new vertical offset in pixels to be applied during the [onDraw] pass.
     */
    fun updateScrollOffset(scrollOffset: Float)

    /**
     * Performs a "soft reset" by invalidating internal layout caches.
     * * This forces the renderer to recalculate dimensions, font sizes, and metric positions
     * during the next frame. Use this when external settings (e.g., column count or font scale)
     * change without needing a full [recycle].
     */
    fun invalidate()

    companion object {
        /**
         * Factory method to instantiate a concrete [SurfaceRenderer] based on the requested type.
         *
         * @param context The Android [Context] used for resource loading and coordinate scaling.
         * @param settings Configuration provider for screen layout, scaling, and visibility.
         * @param metricsCollector The data source containing real-time telemetry metrics.
         * @param fps The tracker used to monitor and display rendering performance.
         * @param surfaceRendererType The specific implementation style to allocate. Defaults to [SurfaceRendererType.GIULIA].
         * @return A concrete instance of [SurfaceRenderer] configured for the specified style.
         */
        fun allocate(
            context: Context,
            settings: ScreenSettings,
            metricsCollector: MetricsCollector,
            fps: Fps,
            surfaceRendererType: SurfaceRendererType = SurfaceRendererType.GIULIA
        ): SurfaceRenderer =
            when (surfaceRendererType) {
                SurfaceRendererType.GAUGE -> GaugeSurfaceRenderer(context, settings, metricsCollector, fps)
                SurfaceRendererType.GIULIA -> GiuliaSurfaceRenderer(context, settings, metricsCollector, fps)
                SurfaceRendererType.DRAG_RACING -> DragRacingSurfaceRenderer(context, settings, metricsCollector, fps)
                SurfaceRendererType.TRIP_INFO -> TripInfoSurfaceRenderer(context, settings, metricsCollector, fps)
                SurfaceRendererType.PERFORMANCE -> PerformanceSurfaceRenderer(context, settings, metricsCollector, fps)
            }
    }
}
