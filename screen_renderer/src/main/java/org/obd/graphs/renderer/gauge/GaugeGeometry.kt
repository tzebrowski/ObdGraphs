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
package org.obd.graphs.renderer.gauge

import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private val NICE_MULTIPLIERS = doubleArrayOf(1.0, 2.0, 2.5, 4.0, 5.0)
private const val MIN_INTERVALS = 4
private const val MAX_INTERVALS = 7
private const val PREFERRED_INTERVALS = 5
private const val MAX_DECIMALS = 4
private const val EPSILON = 1e-9

// A taller card than this only adds empty space around the dial.
internal const val MAX_CARD_HEIGHT_RATIO = 1.5f

/**
 * The dial's scale: [min]..[max] cut into [intervals] steps of [step], all "nice" numbers
 * (1, 2, 2.5, 4 or 5 × 10ⁿ). The range covers the PID's own min..max ([sourceMin], [sourceMax]),
 * extended to the nearest step, so every label sits on a major tick.
 */
internal data class GaugeScale(
    val sourceMin: Double,
    val sourceMax: Double,
    val min: Double,
    val max: Double,
    val step: Double,
    val intervals: Int,
    val decimals: Int
) {
    fun value(index: Int): Double = (min + step * index).let { if (abs(it) < step * EPSILON) 0.0 else it }

    fun label(index: Int): String = String.format(Locale.ROOT, "%.${decimals}f", value(index))

    /** Position of [value] on the dial, 0..1. Values outside the scale stick to its ends. */
    fun fraction(value: Double): Float =
        if (value.isNaN()) 0f else ((value - min) / (max - min)).coerceIn(0.0, 1.0).toFloat()

    companion object {
        fun of(
            sourceMin: Double,
            sourceMax: Double
        ): GaugeScale {
            if (!sourceMin.isFinite() || !sourceMax.isFinite() || sourceMax <= sourceMin) {
                val min = if (sourceMin.isFinite()) sourceMin else 0.0
                return GaugeScale(sourceMin, sourceMax, min, min + 1.0, 1.0, 1, decimals(1.0, min))
            }

            val range = sourceMax - sourceMin
            val baseMagnitude = 10.0.pow(floor(log10(range / MAX_INTERVALS)))

            var best: GaugeScale? = null
            var bestScore = Double.MAX_VALUE
            for (magnitude in doubleArrayOf(baseMagnitude, baseMagnitude * 10)) {
                for (multiplier in NICE_MULTIPLIERS) {
                    val step = multiplier * magnitude
                    val min = floor(sourceMin / step + EPSILON) * step
                    val max = ceil(sourceMax / step - EPSILON) * step
                    val intervals = ((max - min) / step).roundToInt()
                    if (intervals !in MIN_INTERVALS..MAX_INTERVALS) continue

                    // Least extension first: it costs dial resolution. Then the label count.
                    val extension = (max - min - range) / range
                    val score = extension * 1000 + abs(intervals - PREFERRED_INTERVALS)
                    if (score < bestScore - EPSILON) {
                        bestScore = score
                        best = GaugeScale(sourceMin, sourceMax, min, max, step, intervals, decimals(step, min))
                    }
                }
            }

            return best ?: run {
                val step = range / PREFERRED_INTERVALS
                GaugeScale(sourceMin, sourceMax, sourceMin, sourceMax, step, PREFERRED_INTERVALS, decimals(step, sourceMin))
            }
        }

        private fun decimals(
            step: Double,
            min: Double
        ): Int =
            (0..MAX_DECIMALS).firstOrNull { d ->
                val factor = 10.0.pow(d)
                isWhole(step * factor) && isWhole(min * factor)
            } ?: MAX_DECIMALS

        private fun isWhole(value: Double) = abs(value - value.roundToInt()) < 1e-6
    }
}

/**
 * The red parts of the dial, as fractions of the scale: from the PID's upper alert threshold to
 * the end, and from the start to its lower one. None when the PID defines no thresholds.
 */
internal data class GaugeRedZones(
    val lower: Double?,
    val upper: Double?
) {
    fun contains(value: Double): Boolean = (upper != null && value >= upper) || (lower != null && value <= lower)

    fun ranges(scale: GaugeScale): List<ClosedFloatingPointRange<Float>> =
        listOfNotNull(
            lower?.takeIf { it > scale.min }?.let { 0f..scale.fraction(it) },
            upper?.takeIf { it < scale.max }?.let { scale.fraction(it)..1f }
        )

    fun containsFraction(
        scale: GaugeScale,
        fraction: Float
    ): Boolean = ranges(scale).any { fraction in it }

    companion object {
        val NONE = GaugeRedZones(null, null)
    }
}

internal object GaugeGeometry {
    /**
     * Distance from the dial centre to a scale label's centre. The label's outer edge stays on
     * the circle a label at the top would touch, so wide labels at the sides move inward instead
     * of running into the ticks and the card border.
     *
     * @param baseRadius where a label at the top of the dial is centred.
     */
    fun labelCenterRadius(
        baseRadius: Float,
        angleRadians: Double,
        width: Float,
        height: Float
    ): Float {
        val outerRadius = baseRadius + height / 2f
        val radialHalfExtent = abs(kotlin.math.cos(angleRadians)).toFloat() * width / 2f +
            abs(kotlin.math.sin(angleRadians)).toFloat() * height / 2f
        return outerRadius - radialHalfExtent
    }

    /** Text size factor that makes [width] fit [maxWidth]; never enlarges. */
    fun fitScale(
        width: Float,
        maxWidth: Float
    ): Float = if (width <= maxWidth || width <= 0f) 1f else maxWidth / width

    /**
     * Left edges of items laid out in a row centred on [centerX], separated by [gap]. The whole
     * row is scaled down by [StatsRow.scale] when wider than [maxWidth].
     */
    fun statsRow(
        widths: FloatArray,
        gap: Float,
        centerX: Float,
        maxWidth: Float
    ): StatsRow {
        if (widths.isEmpty()) return StatsRow(FloatArray(0), 1f)

        val total = widths.sum() + gap * (widths.size - 1)
        val scale = fitScale(total, maxWidth)
        val lefts = FloatArray(widths.size)
        var x = centerX - total * scale / 2f
        widths.forEachIndexed { i, w ->
            lefts[i] = x
            x += (w + gap) * scale
        }
        return StatsRow(lefts, scale)
    }

    /**
     * Height of a phone gauge card. Square when the grid scrolls anyway; otherwise the rows share
     * [availableHeight], up to [MAX_CARD_HEIGHT_RATIO] × the gauge width.
     */
    fun cardHeight(
        gaugeWidth: Float,
        itemMargin: Float,
        rows: Int,
        availableHeight: Float
    ): Float {
        if (rows <= 0) return gaugeWidth
        val shared = availableHeight / rows - 2 * itemMargin
        return shared.coerceIn(gaugeWidth, gaugeWidth * MAX_CARD_HEIGHT_RATIO)
    }
}

internal class StatsRow(
    val lefts: FloatArray,
    val scale: Float
)
