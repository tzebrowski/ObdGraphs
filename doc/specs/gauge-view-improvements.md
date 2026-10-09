# Spec: Gauge view improvements

2026-10-09 · branch `feat/gauge-view-improvements` · **Status: implemented**

## Summary

Follow-up to [gauge-drawing-improvements](gauge-drawing-improvements.md): a review of `GaugeDrawer` / `GaugeSurfaceRenderer` found four bugs, six readability gaps and three maintainability / performance items.

**Problem.** The progress gradient was set on a paint that never draws the bar. Scale bitmaps (2× resolution, MBs each) of PIDs no longer shown stayed in memory until the drawer was recycled. The value moved sideways with every reading (centred on ink bounds) and jumped when it gained a digit. Red-zone ticks started at whole degrees, off the threshold. The bar jumped between readings, gave no hint of the session range, stayed white in alert, showed nothing for a value past the scale, and a frozen reading looked live. "rate" was hard-coded English, and a long module name ran into it. The text and grid layouts were magic numbers inside Canvas code, untested.

**Scope.** `:screen_renderer` only. `GaugeDrawer` is shared, so the dial changes (items 1–10) also appear on Performance, Drag Racing and Brake Boosting (phone and AA). No preference, query or data changes.

## Behaviour changes

| # | Area | Before | After |
| --- | --- | --- | --- |
| 1 | Progress gradient | `isProgressGradientEnabled()` (always `true`) set a `SweepGradient` on `paint`, which the bar is not drawn with: never visible. Centre passed as (y, x); new shader every frame | Gradient on the bar: white at the dial's start to the bar colour at its end (as Giulia's bars). Cached per PID, rect and colour |
| 2 | Cache memory | Scale bitmap, scale, card gradient kept per PID until `recycle()` | A PID not drawn for 3 s (`EVICT_AFTER_NANOS`) loses them; the bitmap is recycled. Checked at most once per 3 s |
| 3 | Value position | Centred on `getTextBounds` (ink) width, unit included only for values of 4+ characters | Centred on advance width (`measureText`), value and unit always together, tabular digits (`tnum`). Line height from a digit, the same for every value (`--` included) |
| 4 | Red-zone ticks | From `(fraction × sweep).toInt()` | From the exact threshold angle (`GaugeGeometry.zoneTickOffsets`) |
| 5 | Bar motion | Jumped to each reading | Eased, frame-rate independent, time constant 0.12 s; snaps on first draw and after a 1 s gap |
| 6 | Session range | Only in the stats row | Thin marks on the track at min and max (red when that alert was hit); shown when the PID's min / max histogram is enabled |
| 7 | Bar in alert | White bar, theme glow | Bar glow and gradient end in `valueInAlertColor` while the value is in alert |
| 8 | Value off the scale | Bar clamped to the end, nothing else | Small triangle past the dial's end (or before its start) in the bar colour |
| 9 | Stale value | Looked live | No new reading for 10 s (`STALE_AFTER_NANOS`): value and bar grey, no glow. Dials without a value are never stale |
| 10 | Corner captions | `rate 1.2` in English; module name could overlap it | Prefix from `gauge.rate` (EN `rate`, PL `odczyty/s`); module name ellipsized to the space left of the rate |
| 11 | Text layout | Overflow maths inline in `drawStatistics` | `GaugeGeometry.textLayout` (tested); magic numbers named. Same output |
| 12 | Glow cost | Not measurable | `adb shell setprop log.tag.GaugeDrawer VERBOSE` logs average `drawGauge` and glow time every 300 dials |
| 13 | Grid layout | Hand-tuned AA / phone maths inside `GaugeSurfaceRenderer` | `GaugeGrid` (pure, tested). Same output |

## Settings / keys touched

None. New string `gauge.rate` in `screen_renderer/src/main/res/values{,-pl}/strings.xml`.

## Implementation

* `GaugeGeometry.kt`: `GaugeScale.overflow`, `easeNeedle`, `zoneTickOffsets`, `textLayout`, `moduleNameMaxWidth`.
* `GaugeFrameStates.kt` (new, pure, clock passed in): per-PID eased needle, last reading (by `metric.source` identity — the collector replaces it on every reading) and last draw; `evict()` returns the PIDs to drop from the drawer's caches.
* `GaugeGrid.kt` (new): columns, rows, dial width, row height, left / top per index, moved verbatim from `GaugeSurfaceRenderer`.
* `GaugeDrawer`: `drawProgressBar` uses `progressPaint` for the gradient, `progressColor()` for alert / stale, the eased fraction, `drawMinMaxMarkers`, `drawOverflowMarker`. `drawStatistics` uses advance widths. `drawLineTicks` takes float offsets. `evictUndrawn` on every `drawGauge`.
* Easing needs continuous frames: both the phone `RenderingThread` and AA render at the surface frame rate while logging. After the data logger stops, the last frame may show the needle up to one time constant short of the value.
* `MIN_TEXT_VALUE_HEIGHT` stays in px: converting it to dp would move the text on every device without a measured reason.

## Backward compatibility

Visual only. The bar now shows a white-to-colour gradient (item 1) on every gauge, since the (non-configurable) setting is `true`. Short values move left by half the unit's width (item 3). The AA and phone grid and text positions are unchanged (pinned by `GaugeGridTest`, `textLayout` tests).

## Tests

| Test | Pins |
| --- | --- |
| `GaugeGeometryTest` value beyond the scale is reported as overflow | Item 8 |
| needle eases towards the value independent of the frame rate / snaps on the first frame, after a gap and when close | Item 5 |
| red zone ticks start exactly on the threshold / for a zone ending on its step include the end | Item 4 (truncation put the first tick at 66° instead of 66.6°) |
| text sits above the centre by the value line and moves up only when the stats overflow the card | Item 11 |
| module name leaves room for the rate in the other corner | Item 10 |
| `GaugeFrameStatesTest` needle starts on the value, then eases / each PID eases on its own / evicted PID starts afresh | Item 5 |
| dial goes stale when no new reading arrives / without a value is never stale | Item 9 |
| PIDs not drawn any more are evicted / eviction runs at most once per period | Item 2 |
| `GaugeGridTest` AA rows, sizes, starts, overlap; phone portrait and landscape | Item 13 |

Items 1, 3, 6, 7 are Canvas / Paint calls; `:screen_renderer` tests are plain JUnit without Robolectric, so they are on the checklist below.

## Risks and verification checklist

- [ ] Bar shows a white-to-colour gradient on Gauge, Performance, Drag Racing, Brake Boosting (phone + AA DHU); if unwanted, `isProgressGradientEnabled()` is the switch.
- [ ] Value no longer shifts sideways while it changes; `999` → `1000` stays centred with its unit.
- [ ] Bar moves smoothly at 1–2 Hz PIDs; does not lag noticeably on Drag Racing speed.
- [ ] Min / max marks on the track match the stats row; red after an alert.
- [ ] Bar and glow red while the value is in alert.
- [ ] Value above the PID max: triangle past the dial's end.
- [ ] Pull the adapter / stop a PID responding: dial greys after 10 s, recovers on the next reading.
- [ ] Remove a PID from the Gauge screen, scroll a long grid: no crash (recycled bitmap), dials redraw on return.
- [ ] Narrow phone card with rate on: module name ellipsized, not overlapping. Polish locale shows `odczyty/s`.
- [ ] `setprop log.tag.GaugeDrawer VERBOSE` on the DHU: note `drawGauge` / glow averages; decide whether the `BlurMaskFilter` glow stays.

## Out of scope

* Replacing the glow with a cheaper effect: waits for the item 12 numbers.
* A preference for the progress gradient, easing or the stale timeout.
* Blinking the overflow marker: needs a time source in the drawer only for that.
