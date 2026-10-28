# Spec: Recover trip logs that were never saved

2026-10-28 · branch `feat/trip-log-recovery` · Status: implemented, device check pending

## Summary

A trip is recorded to `trips/trip-<profile>-<startTs>.jsonl` and gets its final name,
`trip-<profile>-<startTs>-<lengthSec>.jsonl`, only when it is saved (`saveCurrentTrip()` from the
workflow's `onStopped`). When that never happens — the process is killed or crashes mid-trip, the
phone dies — the file keeps the temporary name. Such a trip:

- is not listed in the trip list (`findAllTripsBy` needs four name segments), so it cannot be opened
  or uploaded by hand;
- was uploaded by auto-sync with duration `0` and a Drive name the log viewers cannot parse
  (`…-<startTs>.jsonl.json.gz`, no length).

Auto-sync also took the trip being recorded (same temporary name) and renamed it to `.synced` under
the open stream. And two races could leave a trip unfinished even when it was stopped normally:
`saveCurrentTrip()` read the trip id and cache in its async block, after a quick restart had already
replaced them (the new trip was "saved", the old file abandoned), and `updateTripMetadata()` cleared
the repository's active id even when a newer trip had started.

Scope: `:datalogger` trip manager/repository, `:integrations` auto-sync. Phone and AA flavours alike.

## Behaviour changes

| Case | Before | After |
| --- | --- | --- |
| Trip cut off (app killed, crash) | Stays `trip-<profile>-<startTs>.jsonl`, hidden from the trip list | Renamed to `trip-<profile>-<startTs>-<len>.jsonl` (length from its last complete metric) when the trip list opens, a new trip starts, or auto-sync runs; listed and uploadable |
| Half-written last line | `loadTrip` failed on it (whole trip unreadable) | Cut off during recovery |
| File with no complete metric | -- | Left as is, logged |
| Auto-sync | Uploaded every non-`.synced` `trip-*` file, including the one being recorded and cut-off ones with duration 0 | Recovers first, then uploads only files whose name carries a length |
| Stop followed at once by a new trip | Async save could finalise the new trip's id; old file left unfinished | The stopping trip's id and data are taken at the call |
| New trip while one is still open | Old trip's file abandoned (and its stream left open) | Old trip saved first; a stream left open is closed |

## Settings / keys touched

None. File names only; the final-name format is unchanged.

## Implementation

- `TripRepository.recoverUnfinishedTrips()` / `FileTripRepository`: files `trip-*.jsonl` with three
  name segments and a numeric start, other than `activeTripId` (now `@Volatile`). Runs on the
  repository's single IO thread (`runBlocking(ioDispatcher)`), so after any queued write, release or
  rename. Reads at most the last 256 KB, walks lines backwards to the last one Jackson parses as a
  `Metric`, truncates after it, names the trip with `entry.x / 1000` (ms since start). Never deletes.
  `finalTripName()` is shared with `updateTripMetadata()`, which now clears `activeTripId` only if it
  is still the finished trip. `initStorage()` closes a stream left open.
- `DefaultTripManager`: `saveCurrentTrip()` captures id + cached trip before `runAsync`;
  `finishTrip()` holds the release / rename / short-trip delete. `startNewTrip()` finishes a trip still
  open, then recovers. `findAllTripsBy()` recovers first. New constructor parameter `repositoryFactory`
  (tests).
- `TripManager.recoverUnfinishedTrips()`; `TripUpload.isReadyForSync()`; `TripCloudSyncWorker`
  recovers, then filters with it.

## Backward compatibility

Trips cut off by earlier versions are recovered the first time the list opens or a trip starts.
Already-uploaded files without a length on Drive are not touched; re-uploading needs the local file
(recovery renames it, auto-sync then uploads it under the proper name).

## Tests

- `FileTripRepositoryTest`: cut-off trip renamed by its last metric with the half line dropped, then
  loads and lists; complete last line without newline kept; recording trip untouched; finishing an
  earlier trip after the next started leaves the new one alone (fails without the `activeTripId` fix);
  unreadable/empty/finished/synced files untouched.
- `DefaultTripManagerTest` (new, fake repository, deferred `runAsync`): stop then immediate start
  saves the first trip's id (fails before); a still-open trip is saved before the next starts (fails
  before).
- `TripUploadTest`: `isReadyForSync` takes finished unsynced trips, skips `.synced`, other files and the
  recording trip.

## Risks and verification checklist

- [x] Unit tests above; the three bug tests fail with the fixes reverted.
- [ ] On a device: kill the app mid-trip, reopen the trip list, check the trip appears with its length
  and uploads. A real cut-off file (`trip-profile_5-1793209935762.jsonl`, 6.3 MB) was converted by
  hand with the same rule (last `entry.x` 743 250 ms → 743 s); its rescaled signals matched the
  ranges of the trips saved normally.
- `recoverUnfinishedTrips()` blocks the caller until the IO queue drains; the trip list and the
  workflow callback wait for pending writes (the tail read itself is a few ms).
- Length is the last metric's time, not wall-clock: a trip with a silent tail reads shorter.
- A recovered trip shorter than the 5 s minimum is kept (the short-trip setting is not applied).

## Out of scope

- Recovering Drive files already uploaded without a length.
- Periodic fsync while recording (writes reach the OS on each metric; only power loss can lose the
  last ones).
