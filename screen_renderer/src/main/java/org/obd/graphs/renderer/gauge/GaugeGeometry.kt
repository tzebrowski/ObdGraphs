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
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private val NICE_MULTIPLIERS = doubleArrayOf(1.0, 2.0, 2.5, 4.0, 5.0)
private const val MIN_INTERVALS = 4
private const val MAX_INTERVALS = 7
private const val PREFERRED_INTERVALS = 5
private const val MAX_DECIMALS = 4
private const val EPSILON = 1e-9

// The needle covers ~63 % of the way to a new value in this time; a whole frame at 60 fps is 0.017 s.
internal const val NEEDLE_TIME_CONSTANT_SECONDS = 0.12f

// A longer gap means the dial was not drawn (scrolled away, screen switched): no easing from a stale needle.
internal const val NEEDLE_MAX_GAP_SECONDS = 1f
private const val SNAP_DISTANCE = 0.001f

// The last quarter of every dial is red (the pre-#225 `dividerHighlightStart = 9` of 12).
internal const val DEFAULT_RED_ZONE_START = 0.75f

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

    /** -1 below the scale, 1 above it, 0 on it. [fraction] clamps, so the dial alone cannot tell. */
    fun overflow(value: Double): Int =
        when {
            value.isNaN() -> 0
            value < min -> -1
            value > max -> 1
            else -> 0
        }

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
 * The red end of the dial: the same share of every dial, whatever the PID or its alert thresholds.
 * Thresholds made it start at a different place on each dial, which looked inconsistent side by side.
 */
internal object GaugeRedZone {
    val range: ClosedFloatingPointRange<Float> = DEFAULT_RED_ZONE_START..1f

    fun contains(fraction: Float): Boolean = fraction in range
}

internal object GaugeGeometry {
    /**
     * Distance from the dial centre to a scale label's centre: [baseRadius], unless the label would
     * then reach past [maxOuterRadius] (into the ticks), in which case it moves just inside it.
     * Pulling every label inward by its width put the end labels into the value / stats area.
     */
    fun labelCenterRadius(
        baseRadius: Float,
        maxOuterRadius: Float,
        angleRadians: Double,
        width: Float,
        height: Float
    ): Float {
        val radialHalfExtent = abs(cos(angleRadians)).toFloat() * width / 2f +
            abs(sin(angleRadians)).toFloat() * height / 2f
        return min(baseRadius, maxOuterRadius - radialHalfExtent)
    }

    /**
     * Widest the min / avg / max row may be. When the dial ends below its centre, the last scale
     * label sits beside that row, so the row must stay clear of the label's inner edge.
     *
     * @param endAngleRadians the dial's end angle (canvas convention: positive is below the centre).
     * @param endLabelRadius distance from the dial centre to the end label's centre.
     */
    fun statsMaxWidth(
        cardLimit: Float,
        endAngleRadians: Double,
        endLabelRadius: Float,
        endLabelWidth: Float,
        gap: Float
    ): Float {
        if (sin(endAngleRadians) <= 0.0) return cardLimit
        val labelInnerX = abs(cos(endAngleRadians)).toFloat() * endLabelRadius - endLabelWidth / 2f
        return min(cardLimit, 2f * (labelInnerX - gap)).coerceAtLeast(0f)
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
     * Offset of the dial's square from the card top that centres what the dial actually draws.
     * A 200° dial draws nothing in the lower part of its circle, so a dial centred as a square
     * left the bottom third of every card empty.
     */
    fun dialTopOffset(
        cardHeight: Float,
        gaugeWidth: Float,
        startAngle: Float,
        sweepAngle: Float
    ): Float {
        val radius = gaugeWidth / 2f
        val (minSin, maxSin) = arcSinRange(startAngle, sweepAngle)
        val drawnHeight = radius * (maxSin - minSin)
        // The drawn part starts radius * minSin below the circle's centre, i.e. this far below its square's top.
        val drawnTop = radius + radius * minSin
        return ((cardHeight - drawnHeight) / 2f - drawnTop).coerceIn(0f, max(0f, cardHeight - gaugeWidth / 2f - radius * maxSin))
    }

    /**
     * How far below the dial square's top the arc's lowest point lies, as a share of the dial width.
     * The default 200° + 180° dial ends 20° below its centre: 0.67.
     */
    fun dialBottomRatio(
        startAngle: Float,
        sweepAngle: Float
    ): Float = (1f + arcSinRange(startAngle, sweepAngle).second) / 2f

    // Lowest and highest sine over the arc (canvas angles: 90 is the bottom, 270 the top).
    private fun arcSinRange(
        startAngle: Float,
        sweepAngle: Float
    ): Pair<Float, Float> {
        val start = Math.toRadians(startAngle.toDouble())
        val end = Math.toRadians((startAngle + sweepAngle).toDouble())
        var minSin = min(sin(start), sin(end)).toFloat()
        var maxSin = max(sin(start), sin(end)).toFloat()
        val from = min(startAngle, startAngle + sweepAngle)
        val to = max(startAngle, startAngle + sweepAngle)
        var a = ceil((from - 90f) / 360f) * 360f + 90f
        while (a <= to) {
            maxSin = 1f
            a += 360f
        }
        a = ceil((from - 270f) / 360f) * 360f + 270f
        while (a <= to) {
            minSin = -1f
            a += 360f
        }
        return minSin to maxSin
    }

    /**
     * Moves the drawn needle [current] towards [target], independent of the frame rate: after
     * [timeConstantSeconds] it has covered ~63 % of the distance. A PID read once or twice a second
     * used to jump straight to each new value. Snaps on the first frame (NaN), after a gap longer
     * than [maxGapSeconds] (the dial was not drawn) and once within [SNAP_DISTANCE].
     */
    fun easeNeedle(
        current: Float,
        target: Float,
        elapsedSeconds: Float,
        timeConstantSeconds: Float = NEEDLE_TIME_CONSTANT_SECONDS,
        maxGapSeconds: Float = NEEDLE_MAX_GAP_SECONDS
    ): Float {
        if (current.isNaN() || elapsedSeconds > maxGapSeconds || timeConstantSeconds <= 0f) return target
        if (elapsedSeconds <= 0f) return current
        val eased = current + (target - current) * (1f - exp(-elapsedSeconds / timeConstantSeconds))
        return if (abs(target - eased) < SNAP_DISTANCE) target else eased
    }

    /**
     * Degrees along the dial at which a red zone's dense ticks go, every [stepDegrees] from the
     * zone's exact start. Truncating the zone ends to whole degrees put the first tick off the
     * threshold on short sweeps.
     */
    fun zoneTickOffsets(
        startFraction: Float,
        endFraction: Float,
        sweepAngle: Float,
        stepDegrees: Float
    ): FloatArray {
        if (stepDegrees <= 0f || endFraction < startFraction) return FloatArray(0)
        val start = startFraction * sweepAngle
        val end = endFraction * sweepAngle
        val count = floor((end - start) / stepDegrees + EPSILON).toInt() + 1
        return FloatArray(count) { start + it * stepDegrees }
    }

    /**
     * Vertical layout of the text inside the dial. Baselines go upward from [centerY]: value and
     * unit share one baseline, the label follows it, the stats row the label. When the stats row
     * would fall below [bottomLimit] (the card's bottom), everything moves up by the overflow.
     */
    fun textLayout(
        centerY: Float,
        valueLineHeight: Float,
        labelLineHeight: Float,
        statsLineHeight: Float,
        verticalGap: Float,
        bottomLimit: Float?
    ): GaugeTextLayout {
        var center = centerY
        if (bottomLimit != null) {
            val predictedStats = center - valueLineHeight + labelLineHeight + verticalGap + statsLineHeight + verticalGap
            val overflow = predictedStats + statsLineHeight * 0.5f - bottomLimit
            if (overflow > 0f) center -= overflow
        }
        return GaugeTextLayout(valueBaseline = center - valueLineHeight)
    }

    /**
     * Widest the module name may be in the card's top-left corner without running into the read
     * rate in the top-right one.
     */
    fun moduleNameMaxWidth(
        cardWidth: Float,
        cornerOffset: Float,
        rateWidth: Float,
        gap: Float
    ): Float = (cardWidth - 2 * cornerOffset - (if (rateWidth > 0f) rateWidth + gap else 0f)).coerceAtLeast(0f)

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

internal data class GaugeTextLayout(
    val valueBaseline: Float
)

internal class StatsRow(
    val lefts: FloatArray,
    val scale: Float
)
