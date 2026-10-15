# Spec: explain the code-history workflow in the DTC list

2026-10-15 · branch `feat/dtc-history-help` (ObdGraphs only) · Status: implemented, on-device check pending

Follow-up to the per-vehicle history (`dtc-history.md`).

## Summary

**Problem.** After Clear Codes, a "No longer reported" section full of the cleared codes appears. The only
explanation was one sentence at the bottom of the collapsed label legend ("codes the previous scan of their module had
and this one has not"). The user could not tell why the section appears, what it compares, or why it is gone after the
next scan.

**Scope.** `:app` only (DTC dialog, adapter, strings EN + PL).

## Behaviour changes

| Area | Before | After |
|---|---|---|
| "No longer reported" header | Title, green check chip with the count, arrow | Also a grey "How it works" chip (question-mark icon). Tapping the chip opens the explanation; tapping the rest of the header still expands / collapses |
| Label legend | Chip table + the one-line "No longer reported" sentence | Also a link "How do New and No longer reported work?" opening the same explanation |
| Explanation dialog | — | "How the code history works": scans saved per vehicle; New; No longer reported after a clear or after healing; worked example (P0100, P0200, Clear Codes); the section lasts one scan; a returning code is not New; unanswered / unchecked modules are left out |

## Settings / keys touched

None. New strings: `dtc.history.help.chip`, `.link`, `.title`, `.body` (EN + PL).

## Implementation

* `DiagnosticTroubleCodeViewAdapter.kt`: `HELP_BADGE`, `showDtcHistoryHelp(context)` (an `AlertDialog`, body from
  `getText` so its `<b>` spans stay). `addMiniBadge` now returns the chip, so a caller can give it its own click;
  `GoneHeaderViewHolder` does that for the help chip.
* `HELP_BADGE` is not a row state, so it is not in `DTC_LEGEND`.
* `dialog_dtc.xml`: `dtc_legend_help` under the legend's gone sentence; wired in `attachLegend`.

## Backward compatibility

UI only; nothing stored.

## Tests

* `DtcHistoryHelpTest` (done): in EN and PL the explanation contains the current "New", "No longer reported" and
  "Clear Codes" labels, and the chip / link / title strings exist. Checked to fail when the example names a button
  label that does not exist.
* `DtcLegendTest` unchanged: the help chip is not a legend entry.
* [ ] Phone: after a clear, the header shows the chip; the chip opens the dialog without toggling the section; the
  header still toggles; the legend link opens the dialog; PL header fits at phone width (title + count + chip + arrow).

## Risks

* PL "Już nie zgłaszane" + count + "Jak to działa" may crowd the header on a narrow phone; the title wraps first
  (weight 1), the chips keep their size.

## Out of scope

* Showing the cleared codes' freeze frames from the gone rows (history keeps them; gone rows have no details).
* An AA explanation (AA has no gone section).
