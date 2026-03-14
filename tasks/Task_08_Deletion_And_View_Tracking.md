can y# Task 08: Deletion Detection and View Tracking Jobs

## Overview

Implement the deletion detection and view tracking layer for Vector9 so the application can mark stale ads as deleted and refresh view statistics for active listings. This task should build on the repository layer from Task 05, the GraphQL client from Task 06, and the crawler foundation from Task 07. It should not yet implement Playwright enrichment or broader production deployment hardening.

**Estimated Time**: 5-7 hours
**Priority**: High
**Dependencies**: Task 05 (Repository Layer and Ad Processing Services), Task 06 (GraphQL Client and Request/Response Models), Task 07 (Crawler Orchestration)

---

## Objectives

1. Add a deletion detection job based on configurable staleness windows
2. Add a view tracking job that fetches `AdViews` for active ads in batches
3. Persist current view counters on `ads` and optionally append snapshots to `view_history`
4. Keep both jobs directly invokable by tests and later scheduler wiring
5. Verify behavior with focused service tests and repository-backed integration coverage

---

## Implementation Notes

- Keep job orchestration code under `src/main/java/md/hashcode/vector9/service` or `scheduler`
- Reuse:
  - `AdRepository`
  - `GraphqlClient`
  - `AdViewsRequest`
  - `view_history` table and generated jOOQ artifacts
- Treat deletion detection as a database-driven staleness policy, not as a GraphQL lookup per ad
- Restrict view fetching to ads that are still active
- Keep jobs callable directly from tests; avoid requiring cron wiring for the core logic
- Make thresholds and batch sizes configurable through application properties
- Prefer idempotent writes so repeated runs do not create invalid state transitions
- Do not add Playwright enrichment, contact scraping, or downstream notifications here

Assumptions carried from the production plan:

- ads missing from fresh listing crawls should be considered deleted by staleness, not by immediate absence in one page fetch
- the default deletion threshold should start at `7` days and remain configurable
- `AdViews` should update `views_total` and `views_since_republish` on `ads`
- `view_history` snapshots are part of the schema already and can be used now without schema changes

---

## Implementation Steps

### Step 1: Add Properties and Service Structure

Create or expand:

```text
src/main/java/md/hashcode/vector9/service/
src/main/java/md/hashcode/vector9/scheduler/
src/main/java/md/hashcode/vector9/model/
```

Recommended class split:

- deletion detection service
- view tracking service
- small job result records for reporting and tests

Suggested class names:

- `DeletionDetectionService`
- `ViewTrackingService`
- `DeletionJobResult`
- `ViewTrackingJobResult`

### Step 2: Implement Deletion Detection

The deletion detection flow should:

1. load ads still marked as active
2. compare `last_seen_at` to the configured staleness threshold
3. mark stale ads as deleted
4. avoid rewriting rows that are already deleted
5. report how many ads were inspected and transitioned

Requirements:

- default stale threshold should be `7` days
- threshold must be configurable through application properties
- deletion updates should be done in batches or through targeted repository methods
- only ads currently in active-like states should be candidates
- repeated runs should not keep mutating the same already-deleted rows

Recommended status behavior for this task:

- set `status=deleted`
- update `updated_at`
- preserve the last known listing data for later analysis

### Step 3: Implement View Tracking

The view tracking flow should:

1. load active ads eligible for view refresh
2. batch ad ids into configurable request sizes
3. call `GraphqlClient.adViews(...)`
4. update current counters on `ads`
5. append optional snapshots to `view_history`
6. capture failures without losing successful batch progress

Requirements:

- batch size should be configurable
- only active ads should be queried
- current counters to update:
  - `views_total`
  - `views_since_republish`
  - `views_last_fetched_at`
- if `today` is present in the response model, update `views_today`; otherwise leave it unchanged
- one failed batch should not discard successful prior batches unless explicitly configured

### Step 4: Add Repository Support

Add or expand repository methods needed by the two jobs.

Likely repository additions:

- fetch active ads older than a staleness cutoff
- mark ads deleted in bulk or one by one
- fetch active ads for view collection in batches
- persist `view_history` snapshots

Keep SQL access inside repositories or focused persistence services.

### Step 5: Define Job Result Models

Add narrow records for job outcomes so later scheduler wiring and tests can inspect behavior cleanly.

Recommended fields:

`DeletionJobResult`
- `adsChecked`
- `adsMarkedDeleted`
- `threshold`
- `failureMessage`

`ViewTrackingJobResult`
- `adsRequested`
- `adsUpdated`
- `historyRowsInserted`
- `batchesAttempted`
- `batchesSucceeded`
- `batchesFailed`
- `failureMessages`

### Step 6: Prepare Scheduler-Ready Entry Points

Keep the core logic in services, but expose methods that later scheduler classes can call directly.

Optional follow-up class names:

- `DeletionDetectionJob`
- `ViewTrackingJob`

For this task:

- scheduler annotations are optional
- direct service invocation is sufficient
- cron wiring can remain a thin follow-up if the service boundaries are clean

### Step 7: Handle Failure Boundaries

Requirements:

- one failed view batch should be reported with enough context
- transport failures from `AdViews` should surface operation and batch ids where practical
- deletion detection failures should not partially hide how many ads were already processed
- result objects should be usable for logs, tests, and future health reporting

---

## Suggested Class Set

The exact names can vary, but this task should likely produce classes close to:

- `DeletionDetectionService`
- `ViewTrackingService`
- `DeletionJobResult`
- `ViewTrackingJobResult`
- `ViewHistoryRepository`

Potential minimal extensions to existing code:

- `AdRepository` methods for stale-ad lookup and status updates
- `GraphqlAdViews` mapping helpers if the current response model needs light adaptation
- application properties for deletion threshold, view batch size, and optional history enablement

---

## Behavioral Requirements

### Deletion Detection

- only stale active ads should be marked deleted
- the staleness cutoff must be configurable
- repeated runs should be safe and idempotent
- ads not yet beyond the threshold must remain active

### View Tracking

- only active ads should be queried for views
- requests should be batched to avoid oversized payloads
- successful responses should update current counters on `ads`
- snapshots should be written to `view_history` when enabled for the task implementation

### Persistence Boundaries

- job code should not embed raw SQL directly
- repositories or dedicated persistence helpers should own update logic
- current ad state and history persistence should stay centralized and testable

### Recovery and Reporting

- successful view batches should remain committed even if a later batch fails
- deletion detection should report exact transition counts
- result objects should make later scheduler and health integration straightforward

---

## Testing Strategy

### Unit and Service Tests

Add focused tests for:

- deletion detection marks only ads older than the configured threshold
- deletion detection ignores already-deleted ads
- view tracking requests only active ads
- view tracking batches ids according to configuration
- view tracking updates ad counters from `AdViews` responses
- view tracking inserts `view_history` snapshots when enabled
- one failed `AdViews` batch does not erase prior successful progress

Recommended test locations:

```text
src/test/java/md/hashcode/vector9/service/
src/test/java/md/hashcode/vector9/integration/
```

Use mocks for:

- `GraphqlClient`
- repositories that are not the subject of the test

### Integration Tests

Add repository-backed tests for:

- marking stale ads deleted
- updating view counters on `ads`
- inserting `view_history` rows with expected timestamps and counters

### Narrow Validation While Iterating

Run focused tests first:

```powershell
mvn -Dtest=DeletionDetectionServiceTest test
mvn -Dtest=ViewTrackingServiceTest test
```

### Full Validation Before Commit

Run:

```powershell
mvn test
```

Expected result:

- deletion and view tracking tests pass
- crawler and repository tests still pass
- no regression in application startup or GraphQL client behavior

---

## Checklist

- [ ] Task 08 task file created
- [ ] deletion detection service implemented
- [ ] stale threshold configuration added
- [ ] view tracking service implemented
- [ ] active-ad batching for `AdViews` added
- [ ] ad counter updates implemented
- [ ] `view_history` persistence implemented or explicitly feature-flagged
- [ ] focused unit/service tests added
- [ ] repository-backed integration coverage added
- [ ] full test suite passes

---

## Deliverables

1. Deletion detection service based on staleness windows
2. View tracking service using `AdViews`
3. Persistence of current counters and history snapshots
4. Automated tests covering service behavior and repository persistence

---

## Next Steps

After completing this task:

1. add thin scheduler classes and cron wiring if not already introduced here
2. add crawl freshness and job-health indicators for operations
3. add Playwright enrichment as the next major collection workstream

---

## Open Questions

1. The production plan mentions `views_today`, while the schema notes also say `view_history` does not include `views_today`.
   Task 08 should treat `view_history` as storing `views_total` and `views_since_republish`, and update `ads.views_today` only if the upstream `AdViews` response provides a stable field for it.

2. The production plan describes scheduler jobs, but Task 08 can keep scheduler annotations thin or optional as long as the service entry points are ready for later cron wiring.

---

**Task Status**: Ready for Implementation
**Estimated Completion**: 5-7 hours
**Blocker For**: Scheduler Wiring, Operations Visibility, and Enrichment Follow-up Tasks
