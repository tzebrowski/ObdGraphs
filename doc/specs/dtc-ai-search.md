# Spec: DTC question for Google AI, with the vehicle

2026-10-25 · branch `feat/dtc-ai-search` · Status: implemented, on-device check pending

## Summary

**Problem.** The expanded DTC row's Google button searched for `OBD2 DTC <code> <description>`: generic results,
nothing about the car, and plain search instead of an explanation.

**Scope.** Phone DTC dialog, the row's search button. It now opens Google AI Mode with a question naming the code, the
vehicle (make, model, engine), the module and the code's state. AA has no such button.

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Button label | "Google Issue" / "Szukaj w Google" | "Ask Google AI" / "Zapytaj AI Google" |
| Opened URL | `google.com/search?q=OBD2 DTC ...` | `google.com/search?udm=50&q=<question>` (AI Mode; elsewhere Google shows plain results) |
| Query text | Code + raw description | Code, description (left out when unknown), failure type, vehicle, module, state, then the question. In the app's language |
| Vehicle settings | Tank size, status panel | Also "Vehicle": make, model, engine (free text) |

## Settings / keys touched

New `pref.vehicle_settings.make`, `.model`, `.engine` (strings, no default, saved per profile like every setting).
Bundled profiles preset make "Alfa Romeo" and the engine (2.0 GME, 1.75 TBI, 2.9 V6 Bi-Turbo, 2.2 MultiJet); the model
is left to the user (one engine fits several models). Default profiles preset nothing.

## Implementation

* `preferences/dtc/DtcSearch.kt`: `dtcAiPrompt` (parts joined in code, string resources drop leading spaces),
  `vehicleDescription`, `dtcAiSearchUrl`, `configuredVehicle()`.
* `configuredVehicle()` reads the active key; a key never set falls back to `<current profile>.<key>`. An upgrade adds
  the preset to the profile's keys only, so without it existing users get no vehicle until they reload the profile.
* The adapter passes the state chip's label (none for a "no longer reported" row) and the failure type line.

## Backward compatibility

No stored data changes. Empty vehicle settings give the question without a vehicle part.

## Tests

`DtcSearchTest`: full prompt, unknown parts left out, vehicle from filled fields only, URL (AI Mode, prompt round
trip), every bundled non-default profile presets make and engine.

## Risks and verification checklist

* [ ] Phone: tap the button on a code: AI Mode opens with the question; PL app gives a Polish question.
* [ ] Settings → Vehicle: the three fields show the profile preset after an upgrade (and after a profile reload).
* [ ] PL button label fits next to the copy button.

## Out of scope

* The VIN (privacy) and freeze frame data (URL length) in the question.
* Decoding make / model year from the VIN.
