# Task 07: Initial and Incremental Crawler Orchestration

## Overview

Implement the crawler orchestration layer for Vector9 so the application can run resumable initial discovery crawls and repeatable incremental refresh crawls on top of the repository layer from Task 05 and the GraphQL client from Task 06. This task should connect subcategory configuration, GraphQL pagination, checkpoint recovery, and ad persistence into a coherent crawl flow. It should not yet implement Spring schedulers or Playwright enrichment.

**Estimated Time**: 6-8 hours
**Priority**: Critical
**Dependencies**: Task 05 (Repository Layer and Ad Processing Services), Task 06 (GraphQL Client and Request/Response Models)

---

## Objectives

1. Implement an initial discovery crawler with checkpoint recovery
2. Implement an incremental crawler with stop-early behavior for already-known ads
3. Connect GraphQL listing fetches to the existing ad persistence service
4. Persist crawl progress and completion state per subcategory
5. Verify crawl behavior with focused service tests and one manual smoke path

---

## Implementation Notes

- Keep crawl orchestration code under `src/main/java/md/hashcode/vector9/crawler` or `service`
- Keep scheduler concerns out of this task; crawlers should be invokable directly by tests or a later scheduler
- Reuse:
  - `SubcategoryRepository`
  - `CrawlCheckpointRepository`
  - `AdPersistenceService`
  - `GraphqlClient`
  - `SearchAdsRequest`
  - `GraphqlAdMapper`
- Initial crawl should page through the full subcategory result set
- Incremental crawl should start from `skip=0` and stop when it is clear the crawler has reached already-known/stale-enough content
- Checkpoint writes should make initial crawl resumable after interruption
- The crawler should process only enabled subcategories from the database
- Keep network request construction explicit per subcategory so `includeCarsFeatures` can vary later
- Do not add `AdViews` collection here unless it falls out naturally as a tiny hook; the main task is listing crawl orchestration
- Do not add deletion detection here; that belongs to a later task driven by staleness policies

Assumptions carried from current implementation:

- normal listing crawls use `includeOwner=true`
- normal listing crawls use `includeBody=false`
- normal listing crawls use `includeBoost=false`
- `SearchAds` is the only operation required for the main crawl loop in this task
- `GraphqlAdMapper` produces the persistence-facing entities needed by `AdPersistenceService`

---

## Implementation Steps

### Step 1: Define Crawl Service Structure

Create or expand:

```text
src/main/java/md/hashcode/vector9/crawler/
src/main/java/md/hashcode/vector9/service/
src/main/java/md/hashcode/vector9/model/
```

Recommended class split:

- a subcategory crawl orchestrator
- an initial crawl service
- an incremental crawl service
- small result/summary records for reporting and tests

Suggested class names:

- `InitialDiscoveryCrawler`
- `IncrementalCrawler`
- `CrawlOrchestrator`
- `CrawlBatchResult`
- `SubcategoryCrawlResult`

### Step 2: Implement Initial Discovery Crawl

The initial crawler should:

1. load enabled subcategories
2. load any existing checkpoint for the subcategory
3. start from checkpoint `skip` or `0`
4. fetch pages through `SearchAds`
5. map ads through `GraphqlAdMapper`
6. persist each processed ad through `AdPersistenceService`
7. update the checkpoint after each successful page
8. clear the checkpoint when the full crawl completes successfully

Requirements:

- page size should be configurable but default to a sensible fixed batch such as `50` or `78`
- the crawler should tolerate count-only differences and rely primarily on returned ads plus paging state
- checkpoint writes should capture at least:
  - `subcategory_id`
  - current `skip`
  - total ads count if available
  - number of ads processed so far
  - last checkpoint timestamp
- if a page returns no ads, the crawler should end cleanly
- if a subcategory fails mid-run, its checkpoint should remain resumable

### Step 3: Implement Incremental Crawl

The incremental crawler should:

1. load enabled subcategories
2. always start from `skip=0`
3. fetch fresh pages from `SearchAds`
4. persist mapped ads through `AdPersistenceService`
5. stop early once the page indicates the crawler is fully back in already-known territory

Recommended stop-early policy for this task:

- stop when the crawler encounters `5` consecutive unchanged ads by default
- make the threshold configurable via application properties for later tuning
- reset the unchanged counter whenever a new or materially updated ad is processed

Implementation detail:

- if `AdPersistenceService` does not yet expose enough result detail for stop-early decisions, extend its result model minimally rather than embedding repository lookups into the crawler

### Step 4: Add Category-Aware Request Building

Build `SearchAdsRequest` per subcategory using database metadata and task defaults.

The request builder should support:

- subcategory id
- pagination limit and skip
- locale/language defaults
- `includeOwner=true`
- `includeBody=false`
- `includeBoost=false`
- `includeCarsFeatures=true` only when the subcategory requires it

If the `subcategories` table already stores transport-specific flags, use them directly.

### Step 5: Define Result and Reporting Models

Add narrow records for crawler output so tests and later schedulers can inspect behavior without scraping logs.

Recommended fields:

`SubcategoryCrawlResult`
- `subcategoryId`
- `adsFetched`
- `adsProcessed`
- `newAds`
- `updatedAds`
- `unchangedAds`
- `pagesFetched`
- `completed`
- `stoppedEarly`
- `failureMessage`

`CrawlBatchResult`
- total subcategories attempted
- total successes
- total failures
- per-subcategory results

### Step 6: Handle Errors and Recovery Boundaries

Requirements:

- a single failing subcategory should not abort the entire batch unless explicitly configured
- subcategory failures should be captured in the result object and logs
- initial crawl should preserve checkpoint state on failure
- incremental crawl should not create or mutate checkpoints unless there is a clear reason to track them separately
- GraphQL transport exceptions should be surfaced with subcategory context

### Step 7: Add Manual Smoke Path

Add a disabled manual test or dev-only entry point that runs one real subcategory crawl against:

- `https://999.md/ro/list/computers-and-office-equipment/mini-pc?o_16_1=776`

The smoke path should:

- execute one page or one short crawl
- print or serialize the crawl result summary
- optionally persist to the test database if that setup is already present
- avoid becoming part of the normal automated suite

---

## Suggested Class Set

The exact names can vary, but this task should likely produce classes close to:

- `InitialDiscoveryCrawler`
- `IncrementalCrawler`
- `CrawlOrchestrator`
- `SubcategoryCrawlResult`
- `CrawlBatchResult`
- `SearchAdsRequestFactory`

Potential minimal extensions to existing code:

- `ProcessedAdResult` enriched with enough detail for incremental stop-early logic
- `SubcategoryRepository` methods for enabled crawl targets
- `CrawlCheckpointRepository` helpers for resume and clear flows
- crawler properties class for page size and unchanged-ad stop threshold

---

## Behavioral Requirements

### Initial Crawl

- resumes from the saved checkpoint when one exists
- processes all available pages for the subcategory
- updates checkpoint progress as it goes
- clears checkpoint on successful completion

### Incremental Crawl

- always begins at the newest listings (`skip=0`)
- processes recent pages first
- stops before scanning the full category when `5` consecutive unchanged ads are encountered by default
- does not depend on scheduler logic

### Persistence Integration

- every fetched ad should flow through the existing persistence service
- owner, ad, image, and price history behavior should remain centralized in Task 05 code
- crawler code should not write SQL directly

### Idempotency and Recovery

- repeated incremental runs should be safe
- interrupted initial crawls should resume without duplicating progress semantics
- unchanged ads should not create redundant side effects beyond expected `last_seen_at` refreshes

---

## Testing Strategy

### Unit and Service Tests

Add focused tests for:

- initial crawl starts from checkpoint skip
- initial crawl updates checkpoint after each page
- initial crawl clears checkpoint on success
- initial crawl preserves checkpoint on failure
- incremental crawl stops early after the configured number of consecutive unchanged ads
- only enabled subcategories are crawled
- category flags influence `SearchAdsRequest` correctly
- GraphQL failures are isolated to the failing subcategory result

Recommended test locations:

```text
src/test/java/md/hashcode/vector9/crawler/
src/test/java/md/hashcode/vector9/integration/
```

Use mocks for:

- `GraphqlClient`
- `AdPersistenceService`
- repositories needed for orchestration decisions

### Narrow Validation While Iterating

Run focused tests first:

```powershell
mvn -Dtest=InitialDiscoveryCrawlerTest test
mvn -Dtest=IncrementalCrawlerTest test
```

### Full Validation Before Commit

Run:

```powershell
mvn test
```

Expected result:

- crawler orchestration tests pass
- existing repository and GraphQL tests still pass
- no Spring boot or integration regressions are introduced

---

## Checklist

- [ ] Task 07 task file created
- [ ] crawler package structure added or expanded
- [ ] initial discovery crawler implemented
- [ ] checkpoint resume and clear behavior implemented
- [ ] incremental crawler implemented
- [ ] stop-early policy covered by tests
- [ ] category-aware request building implemented
- [ ] crawl result summary models added
- [ ] manual smoke path added
- [ ] full test suite passes

---

## Deliverables

1. Initial discovery crawl orchestration with checkpoint recovery
2. Incremental crawl orchestration with stop-early behavior
3. Integration between GraphQL listing fetches and persistence services
4. Automated crawler tests plus one disabled manual smoke path

---

## Next Steps

After completing this task:

1. add scheduler jobs to invoke initial and incremental crawlers
2. add deletion detection and stale-ad lifecycle handling
3. add `AdViews` collection and view-history persistence if kept in Phase 1
4. add Playwright enrichment as a separate asynchronous workstream

---

## Open Questions

1. The production plan mentions `source=desktop_redesign`, while the captured mini-PC traffic and current Task 6 implementation use `source=desktop` with GraphQL input `AD_SOURCE_DESKTOP`.
   Task 07 should keep the Task 6 behavior unless you want to standardize that later.

---

**Task Status**: Ready for Implementation
**Estimated Completion**: 6-8 hours
**Blocker For**: Scheduler and Operational Crawl Tasks
