# Task 10: Playwright Detail Enrichment

## Overview

Implement the Playwright-based detail enrichment layer for Vector9 so the application can fetch ad pages selectively, extract fields that are not exposed reliably through GraphQL, and persist the structured results for later analysis. This task should build on the crawler, deletion, view tracking, and observability foundations from Tasks 07, 08, and 09. It should not yet implement downstream alert delivery such as Telegram notifications or reporting workflows.

**Estimated Time**: 7-10 hours
**Priority**: High
**Dependencies**: Task 05 (Repository Layer and Ad Processing Services), Task 07 (Crawler Orchestration), Task 08 (Deletion and View Tracking Jobs), Task 09 (Production Observability and Deployment Hardening)

---

## Objectives

1. Add a Playwright runtime that can fetch and parse ad detail pages
2. Add selective enrichment targeting for new, changed, or not-yet-enriched ads
3. Persist structured enrichment output into existing enrichment-friendly storage
4. Track enrichment success, failure, retryability, and timestamps on ads
5. Verify the enrichment flow with focused parser/service tests and one manual smoke path

---

## Implementation Notes

- Keep browser and extraction code under `src/main/java/md/hashcode/vector9/enrichment`
- Reuse existing `ads` enrichment fields and `ad_attributes` table rather than creating a new storage model unless a hard schema gap appears
- Treat enrichment as asynchronous follow-up work after list crawling, not part of the main GraphQL crawl path
- Keep extraction logic category-aware, but start with a common generic pipeline plus targeted extractors where needed
- Prefer externally configurable timeouts, concurrency, retry limits, and refresh windows
- Add a clean boundary between:
  - selecting ads to enrich
  - loading and rendering the page
  - extracting structured values
  - persisting enrichment output
- Prefer parsing embedded page state from the main ad HTML response before relying on brittle DOM-only selectors
- Do not implement Telegram alerts, reporting, or public API access in this task

Assumptions carried from the production plan:

- Playwright should be used only when GraphQL is insufficient or when enrichment is explicitly required
- eligible ads include newly discovered ads, price-changed ads, not-yet-enriched ads, and failed ads that are retryable
- enrichment output should include category-specific characteristics, geolocation/address data where available, and contact information where available
- failed enrichments should remain visible and retryable without blocking crawl freshness

Confirmed from local capture material:

- the main ad page HTML can contain embedded Next.js payload data in `self.__next_f.push(...)`
- that embedded payload includes a structured `adView` object for at least the sampled Mini PC ad captures
- the `adView` payload contains grouped controls with human-readable titles and feature values, for example:
  - `Memorie RAM` -> `16 GB`
  - `Capacitate de stocare` -> `512 GB`
  - `Model procesor` -> `M2 Pro`
  - `Sistem de operare` -> `MacOs`
- contact information such as phone numbers can be present in the embedded payload
- this makes HTML response parsing a valid primary enrichment path even before deeper DOM scraping is needed

Recommended extraction posture for Task 10:

- primary path:
  - fetch the ad HTML through Playwright
  - extract the embedded `adView` payload from the Next.js page data
  - map grouped controls and feature values into structured enrichment output
- secondary path:
  - use DOM selectors only for fields not present in the embedded page payload

Recommended persistence posture for Task 10:

- persist the full grouped attribute structure into `ad_attributes` as the canonical raw enrichment payload
- also persist a small normalized subset of stable fields for easier querying when those mappings are obvious
- include both human-readable labels and raw feature ids where available so future remapping remains possible

---

## Implementation Steps

### Step 1: Add Enrichment Configuration and Runtime Setup

Create or expand:

```text
src/main/java/md/hashcode/vector9/enrichment/
src/main/resources/
```

Recommended additions:

- Playwright/browser runtime wrapper
- enrichment properties class
- browser lifecycle management
- basic page fetch result model

Suggested property areas:

- `vector9.enrichment`
- `vector9.enrichment.retry`

Expected configurable settings:

- enabled flag
- max ads per run
- page timeout
- navigation timeout
- concurrency limit
- retry limit
- refresh age for re-enrichment

### Step 2: Implement Candidate Selection

The enrichment selector should identify ads that need detail scraping.

Recommended initial candidates:

1. ads with `details_enriched=false`
2. ads with `enrichment_status=failed` and retry budget remaining
3. ads whose details are stale enough for refresh
4. optionally ads with recent price changes if that signal is already available cleanly

Requirements:

- selection should be repository-driven
- active ads should be prioritized
- deleted ads should normally be excluded unless a clear reason exists
- batch size and ordering should be configurable
- repeated runs must be safe and should not re-enrich the same ad concurrently

### Step 3: Implement Page Fetching and Extraction

For each selected ad:

1. build or load the detail page URL
2. open the page through Playwright
3. wait for stable page state using a bounded timeout
4. extract structured values
5. capture enough raw context for troubleshooting without storing full HTML by default

Extraction targets should include:

- category-specific characteristics
- address or location fields when present
- contact-related fields when present
- normalized page state useful for enrichment auditing

For the first implementation, prefer extracting from the embedded page payload when available. A good first target model for the extracted result is:

- ad identity and URL
- owner summary
- contact summary
- grouped attributes:
  - group title
  - control title
  - control type
  - feature id
  - feature type
  - translated value
  - raw value
- normalized selected fields where obvious, for example:
  - `condition`
  - `seller_type`
  - `cpu_series`
  - `cpu_model`
  - `ram`
  - `storage_capacity`
  - `operating_system`
  - `power_watts`
  - `region`
  - `phone_numbers`

Requirements:

- missing fields should be handled gracefully
- selectors and parsing should be defensive against partial page changes
- the extractor should return structured data rather than writing SQL directly
- the code should support category-specific extractors later without rewriting the core pipeline
- when parsing embedded page data, the extractor should keep enough raw structure to survive future label-mapping changes

### Step 4: Persist Enrichment Output

Persist the extracted detail data using existing schema support.

Likely persistence behavior:

- update `ads.details_enriched`
- update `ads.enrichment_status`
- increment `ads.enrichment_attempts`
- set `ads.enrichment_last_attempt_at`
- set `ads.details_last_enriched_at` on success
- write extracted structured attributes into `ad_attributes`
- persist transport-specific feature data into `car_features` when relevant and justified by the extracted output

Recommended first-pass storage model:

- `ad_attributes` should store:
  - raw grouped attributes extracted from the page payload
  - normalized selected fields for easier querying
  - extraction metadata such as source type (`embedded_page_state` vs `dom_fallback`)
- if category-specific normalization is still evolving, prefer storing richer raw payload rather than forcing premature flattening
- `car_features` should remain optional in this task unless the transport mapping is already clearly supported by the extracted payload

Requirements:

- success and failure outcomes must be durable
- persistence should be idempotent for repeated successful enrichments
- old enrichment values should be refreshed cleanly instead of duplicated when the schema expects current-state semantics

### Step 5: Define Retry and Failure Policy

Implement explicit retry boundaries.

Requirements:

- retryable failures should remain distinguishable from permanent failures
- retry budget should be configurable
- one failed ad should not abort the whole batch
- result objects should capture:
  - ads attempted
  - ads enriched successfully
  - ads failed
  - ads skipped
  - retryable failures

Recommended failure categories:

- navigation timeout
- selector/extraction failure
- blocked or unexpected page state
- persistence failure

### Step 6: Expose Scheduler-Ready Service Entry Points

Keep the core enrichment logic invokable directly from tests and later scheduler wiring.

Suggested class names:

- `DetailEnrichmentService`
- `EnrichmentCandidateSelector`
- `PlaywrightDetailFetcher`
- `AdDetailExtractor`
- `EnrichmentJobResult`

For this task:

- direct service invocation is sufficient
- scheduler annotations are optional
- service boundaries should be ready for later cron wiring

### Step 7: Add Manual Smoke Path

Add a disabled manual smoke test or dev-only entry point that:

- runs enrichment for one known ad detail page
- prints or serializes the extracted enrichment result
- can use the test profile and Testcontainers setup where practical
- stays outside the normal automated suite

---

## Suggested Class Set

The exact names can vary, but this task should likely produce classes close to:

- `DetailEnrichmentService`
- `EnrichmentCandidateSelector`
- `PlaywrightDetailFetcher`
- `AdDetailExtractor`
- `EnrichmentJobResult`
- `EnrichmentProperties`

Potential minimal extensions to existing code:

- repository methods for selecting enrichment candidates and persisting enrichment state
- a small structured model for extracted attributes
- optional category-specific extractor strategy interfaces
- observability hooks so enrichment later fits into existing metrics and freshness reporting

---

## Behavioral Requirements

### Candidate Selection

- not-yet-enriched ads should be eligible
- retryable failed ads should be eligible until the retry budget is exhausted
- already-fresh enriched ads should be skipped
- repeated runs should not create duplicate concurrent work on the same ad

Recommended initial priority order:

1. `details_enriched = false`
2. retryable failed enrichments
3. stale enriched ads eligible for refresh

Price-changed ads can remain a later extension unless the current schema already exposes that signal cleanly.

### Extraction

- enrichment should tolerate missing or partially rendered fields
- generic extraction should work across categories, with extension points for category-specific logic
- transport-specific data should map cleanly when present
- page fetch failures should not crash the whole job
- for pages like the captured Mini PC ad, extraction should prefer the embedded `adView.groups[].controls[]` data rather than reconstructing values from visible text only
- raw numeric feature ids and decoded labels should both be retained where possible

### Persistence

- successful enrichments should mark the ad as enriched and update timestamps
- failed enrichments should record attempt metadata and status
- extracted attributes should be queryable later from normalized storage
- repeated successful enrichment should refresh current state rather than create invalid duplicates

### Recovery and Reporting

- one failing ad should not discard successful enrichments from earlier ads in the same run
- result objects should support later scheduler, health, and metric integration
- manual smoke execution should remain available without becoming part of the automated suite

---

## Testing Strategy

### Unit and Service Tests

Add focused tests for:

- candidate selection for not-yet-enriched and retryable failed ads
- retry budget behavior
- extractor behavior on representative HTML fragments or stored test fixtures
- persistence of success and failure state updates
- category-specific extraction extensions where introduced
- parsing of embedded Next.js page payload into grouped attributes and normalized selected fields

Recommended test locations:

```text
src/test/java/md/hashcode/vector9/enrichment/
src/test/java/md/hashcode/vector9/service/
src/test/resources/
```

Use mocks or fixtures for:

- Playwright/browser interactions where a real browser is not needed
- repository collaborators not under direct test

Recommended fixture sources for this task:

- `tasks/mini-pc-ad-103748724-other/0001-response-body.html`
- the existing `mini-pc-ad-*` and `mini-pc-listings` captures for cross-checking field labels and feature ids

### Integration Tests

Add coverage for:

- repository-backed enrichment state persistence
- application startup with enrichment configuration enabled
- one bounded Playwright-backed enrichment path if feasible in CI; otherwise keep this as a disabled manual smoke path

### Narrow Validation While Iterating

Run focused tests first:

```powershell
mvn -Dtest=DetailEnrichmentServiceTest test
mvn -Dtest=AdDetailExtractorTest test
```

### Full Validation Before Commit

Run:

```powershell
mvn test
```

Expected result:

- enrichment tests pass
- existing crawler, repository, and observability tests still pass
- no regression in application startup or current production profile defaults

---

## Checklist

- [ ] Task 10 task file created
- [ ] Playwright runtime wrapper added
- [ ] enrichment candidate selection implemented
- [ ] detail extraction pipeline implemented
- [ ] enrichment persistence wired
- [ ] retry and failure handling implemented
- [ ] disabled manual smoke path added
- [ ] focused tests added
- [ ] full test suite passes

---

## Deliverables

1. Playwright-based detail enrichment runtime and service flow
2. Candidate selection and retry-aware enrichment orchestration
3. Persistence of structured enrichment output and enrichment state
4. Automated tests plus one disabled manual enrichment smoke path

---

## Next Steps

After completing this task:

1. add thin scheduler wiring for recurring enrichment runs if not included here
2. add downstream notifications or reporting on top of the now-complete crawl and enrichment data
3. expand category-specific extractors once the first production targets stabilize

---

## Open Questions

1. The current schema supports flexible enrichment through `ad_attributes`, but category-specific normalization depth can vary.
   Task 10 should start with generic structured persistence plus targeted normalization only where the value is clear and the extractor is stable.

2. Playwright in CI and local developer machines may require browser installation and platform-specific setup.
   Task 10 should keep real-browser validation narrow and provide a disabled manual smoke path if always-on browser execution would make the automated suite fragile.

3. The newly captured ad HTML shows that important detail data is embedded in the page payload itself.
   Task 10 should treat embedded page-state parsing as the default extraction source, with DOM scraping as a fallback rather than the primary implementation.

---

**Task Status**: Ready for Implementation
**Estimated Completion**: 7-10 hours
**Blocker For**: Enrichment Scheduling, Notifications, and Downstream Consumer Features
