# Spec: DTC state (active / stored / pending / history) and warning lamp

2026-10-13 · branch `feat/dtc-status-badges` · Status: implemented, manual checks pending

## Summary

**Problem.** Every code looked equally urgent. The ECU's status byte was shown only raw: the hex mask and the list of
set bits ("Test Failed, Confirmed DTC, Test Failed Since Last Clear, ...") in the phone's expanded row, and the same
bit list on AA. Other diagnostic apps (OBDeleven, FORScan, Car Scanner) reduce it to one state per code, so the
driver sees at a glance whether a fault is present now, stored, still pending, or past, and whether it lights the MIL.

**Scope.** `:datalogger` (status decoding, sort, shared), `:app` (state chips + explanation, share report, EN/PL),
`:automotive` (row icon tint and status line, EN/PL). No ObdMetrics change: every code comes from its UDS parser,
so `DiagnosticTroubleCode.statusMask` is always the ISO 14229-1 status byte.

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Phone code row, collapsed | Code + description | + a chip line below code and description: state chip (icon + label) and a separate MIL chip |
| Phone code row, expanded | Hex, mask, bit list | + one line per chip explaining it ("Active: the fault is present right now."), raw detail stays |
| AA code row | Red icon; bit list in yellow | Icon and status line tinted by state: active red, stored yellow, pending / history plain |
| Phone dialog top (added 2026-10-14) | Chip meaning only in an expanded row | "What do the labels mean?" toggle under the readiness row opens a legend: each chip (New, Active, Stored, Pending, History, MIL) as rows show it, with its meaning, plus a "No longer reported" line. Collapsed by default; the choice is kept |
| Order within a module (phone + AA) | Known descriptions first, then code | Active, stored, pending, history, no state; then known descriptions first, then code |
| Shared report | Status bit list + hex | + "State: Active · MIL on" line |

State, from the status byte (most urgent wins):

| Bits set | State |
|---|---|
| `0x01` testFailed | Active |
| `0x08` confirmedDTC | Stored |
| `0x04` pendingDTC | Pending |
| `0x02` / `0x20` failed this cycle / since last clear | History |
| none of the above | no state, no line |

`0x80` warningIndicatorRequested adds "MIL on", independent of the state (a lamp with no fault bit shows alone).

Phone chips: icon, colour and label all differ per state, so the state never rests on colour alone. Chip colours are
their own palette (`dtc_state_*`, light + night), none reuses the codes' red or the module header's amber:

| Chip | Icon | Colour |
|---|---|---|
| Active | error (circle !) | orange |
| Stored | save (disk) | blue |
| Pending | hourglass | violet |
| History | history (clock) | grey |
| MIL | lamp | yellow |

Light theme: solid fill with white text (every light colour ≥ 4.5:1 against white); night: faint fill (20 %) with
coloured text (`R.bool.dtc_badge_solid`). A faint fill on the light dialog washed out to pink / beige.

Wrong turn: a bold coloured line under the description ("Aktywny · MIL zapalona") competed with the description and
reused the codes' red; most rows read the same red word. Replaced by the chips. Second wrong turn: chips stacked
under the code were wider than the code column, overlapped the description and ran into the divider. Third: barriers
placing the chips below code + description pushed them far down in an expanded row.

## Settings / keys touched

`pref.dtc.legend.expanded` (boolean, default false): legend open or closed. New strings `dtc.state.*` (app + automotive), `dtc.state.mil`, `dtc.state.*.explanation` and
`dtc.share.state` (app), EN + PL.

## Implementation

* `datalogger/.../dtc/DtcStatus.kt`: `dtcStatus(statusMask)` → `DtcStatus(state, warningLamp)`; `statusLine(labels)`
  builds the localized line. `DtcState` declaration order is the display order.
* `sortedForDisplay()` sorts by state after module. `toDtcListItems` keeps that order inside each module group.
* Phone: `dtc_badges` (state + MIL chips side by side, style `DtcBadge`, `dtc_badge_background` tinted in code) on a
  line of its own below the description in `item_dtc.xml` (top-down stack: description → chips → details → divider,
  no barriers), gone when it holds no chip; `dtc_detail_state` in the expanded details. Per-state colour / icon / explanation is `STATE_BADGES`
  in the adapter. `dtcStatusLabels(context)` is shared by the adapter and the share report.
* AA: `DiagnosticTroubleCodesScreen.row()` tints the icon and status line by state.
* Legend (phone, added after users found the chips unclear): `dtc_legend_toggle` + `dtc_legend` in `dialog_dtc.xml`,
  rows `item_dtc_legend_row.xml` (`TableRow`s in `dtc_legend_table`) built from `DTC_LEGEND` (label + `BadgeStyle`,
  row order) with the adapter's `showBadge`, so a chip looks the same in the legend and the list. The table makes the
  chip column as wide as the widest chip, so explanations start in one column. Texts reuse the `*.explanation` strings minus
  their "Label: " prefix (`legendExplanation`) — one wording to maintain. Strings `dtc.legend.toggle`, `dtc.legend.gone`.

## Backward compatibility

Stored codes keep their `statusMask` (serialized with the list), so codes read before this change get a state too.
A mask of 0 shows no line, as before.

## Tests

* `DtcStatusTest`: each state, priority between bits, lamp independence, status line.
* `DtcListItemTest`: order by state within a module.
* `DtcLegendTest` (app): the legend covers every chip once; the label prefix is dropped.

## Risks and verification checklist

* [ ] Phone, light (solid chips) and dark (faint chips) theme: chips readable, icons tinted, code column not widened by "OCZEKUJĄCY".
* [ ] Phone: a row with no state has no chips and no explanation line.
* [ ] Phone: expanding a row keeps the chips right under the description.
* [ ] AA (DHU): icon tints, two text lines (description + status) per row, no truncation of "Active · MIL on" / PL.
* [ ] Real ECM: an active fault with the engine running shows "Active"; after it heals, "Stored".
* ECUs that do not support some bits (status availability mask) may report states less precisely; the mask is not
  per code and is ignored.

## Out of scope

Mode 07 / 0A (pending / permanent) generic reads, readiness monitors, per-VIN scan history, filtering by state.
