# Task 05: Repository Layer and Ad Processing Services

## Overview

Implement the persistence layer for Vector9 on top of the schema from Task 03 and the generated jOOQ classes from Task 04. This task should introduce repository classes and the first service-level ad processing flow so later GraphQL and crawler tasks can persist ads, owners, images, price changes, and checkpoints through stable application APIs instead of writing SQL inline.

**Estimated Time**: 4-6 hours
**Priority**: Critical
**Dependencies**: Task 04 (jOOQ Code Generation and Database Access Setup)

---

## Objectives

1. Add jOOQ-backed repositories for the core crawler tables
2. Implement idempotent upsert behavior for owners, ads, images, and checkpoints
3. Implement price-change detection with `price_history` persistence
4. Add a service that coordinates ad persistence through the repositories
5. Verify repository behavior with integration tests against the migrated schema

---

## Implementation Notes

- Use the generated jOOQ classes under `md.hashcode.vector9.jooq`
- Keep repository code in `src/main/java/md/hashcode/vector9/repository`
- Keep service orchestration in `src/main/java/md/hashcode/vector9/service`
- Use Spring-managed `DSLContext`; do not open manual JDBC connections in repositories
- Keep repository contracts focused on persistence and lookup logic, not GraphQL DTO parsing
- Preserve current schema decisions from the production plan:
  - `owners.id` remains the primary foreign key reference from `ads`
  - `ad_images` is append-only
  - duplicate images are skipped by filename
  - `view_history` does not include `views_today`
- Do not implement the GraphQL client in this task
- Do not implement schedulers or crawl loops in this task

---

## Implementation Steps

### Step 1: Add Persistence Package Structure

Create or expand:

```text
src/main/java/md/hashcode/vector9/repository/
src/main/java/md/hashcode/vector9/service/
src/main/java/md/hashcode/vector9/model/
```

Recommended additions:

- repository interfaces only if they add value now; concrete jOOQ repositories are acceptable
- service classes for orchestration
- simple command/request records for persistence input if needed

### Step 2: Implement Core Repositories

Add repositories for:

- `subcategories`
- `owners`
- `ads`
- `ad_images`
- `price_history`
- `crawl_checkpoints`

Expected repository responsibilities:

`SubcategoryRepository`
- list enabled subcategories
- fetch subcategory by id

`OwnerRepository`
- upsert by owner UUID
- update mutable metadata such as avatar, verification, plan, and last-updated timestamp
- support lookup by UUID

`AdRepository`
- upsert ad core fields by ad id
- update tracking fields such as `last_seen_at`, `status`, `details_enriched`, `enrichment_status`
- support lookup by ad id

`AdImageRepository`
- read existing filenames for an ad
- insert only new filenames
- preserve old filenames already stored

`PriceHistoryRepository`
- insert a history row only when the effective price changes

`CrawlCheckpointRepository`
- fetch checkpoint by subcategory
- create or update checkpoint progress
- clear checkpoint when a full crawl completes if that policy is chosen

### Step 3: Define Service-Level Ad Processing Flow

Implement an `AdPersistenceService` or similarly named service that coordinates:

1. owner upsert
2. current ad lookup
3. ad upsert
4. price change detection and optional `price_history` insert
5. image refresh using append-only filename deduplication
6. `last_seen_at` refresh

Requirements:

- repeated processing of the same unchanged ad must be idempotent
- price history should record one row per actual change
- processing should be transactional at the service level
- repository/service code should not assume GraphQL body text is always present

### Step 4: Keep Input Models Narrow

Introduce service input records only for the fields actually needed now.

Recommended examples:

- `OwnerUpsertCommand`
- `AdUpsertCommand`
- `AdImageInput`
- `CheckpointUpdateCommand`

These should be internal persistence-facing models, not upstream API models.

### Step 5: Add Repository and Service Tests

Add integration tests that verify:

- owner upsert inserts and then updates by UUID
- ad upsert is idempotent
- duplicate image filenames are not inserted twice
- price history is created exactly once when the price changes
- checkpoint updates persist correctly
- the service flow writes owner, ad, and image data together

Recommended test locations:

```text
src/test/java/md/hashcode/vector9/integration/repository/
src/test/java/md/hashcode/vector9/integration/service/
```

Use PostgreSQL/Testcontainers plus Flyway migrations, as in the existing schema tests.

---

## Suggested Class Set

The exact names can vary, but the task should likely produce classes close to:

- `SubcategoryRepository`
- `OwnerRepository`
- `AdRepository`
- `AdImageRepository`
- `PriceHistoryRepository`
- `CrawlCheckpointRepository`
- `AdPersistenceService`

Optional helper records:

- `OwnerUpsertCommand`
- `AdUpsertCommand`
- `ProcessedAdResult`

---

## Behavioral Requirements

### Owner Handling

- treat owner UUID as the stable technical identity
- update mutable fields on repeated sightings
- do not create duplicate owners when the same UUID is seen again

### Ad Handling

- upsert by ad id
- update mutable listing fields and `updated_at`
- refresh `last_seen_at` on every successful processing pass
- leave deletion marking for a later deletion-detection task

### Price History

- compare normalized current price fields against the stored row
- insert into `price_history` only when the effective price changed
- do not insert duplicate history rows for identical repeated crawls

### Images

- preserve all existing stored images
- insert only new filenames
- skip duplicates by filename even if the same image appears again in the input

### Checkpoints

- support resumable initial crawl progress by subcategory id
- keep checkpoint writes idempotent

---

## Testing Strategy

### Narrow Validation While Iterating

Run focused tests first:

```powershell
mvn -Dtest=OwnerRepositoryIntegrationTest test
mvn -Dtest=AdPersistenceServiceIntegrationTest test
```

### Full Validation Before Commit

Run:

```powershell
mvn test
```

Expected result:

- all repository and service integration tests pass
- existing schema and jOOQ tests still pass
- no transactional regressions are introduced

---

## Checklist

- [ ] Task 05 task file created
- [ ] repository package structure added or expanded
- [ ] core jOOQ repositories implemented
- [ ] service-level ad persistence flow implemented
- [ ] owner upsert behavior covered by tests
- [ ] ad upsert behavior covered by tests
- [ ] append-only image deduplication covered by tests
- [ ] price history behavior covered by tests
- [ ] checkpoint persistence covered by tests
- [ ] full test suite passes

---

## Deliverables

1. jOOQ-backed repository layer for core crawler entities
2. service-level ad persistence orchestration
3. integration tests for repository and ad-processing behavior
4. a persistence API ready for the GraphQL ingestion task

---

## Next Steps

After completing this task:

1. implement the GraphQL client and request/response models
2. map GraphQL listing payloads into the persistence commands
3. connect crawler orchestration to the new persistence layer

---

**Task Status**: Ready for Implementation
**Estimated Completion**: 4-6 hours
**Blocker For**: GraphQL Client and Crawler Tasks
