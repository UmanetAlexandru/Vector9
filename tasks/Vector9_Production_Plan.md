# Vector9 Production Plan

## 1. Executive Summary

Vector9 is a production-oriented scraping and tracking platform for `999.md`, built with Java 21 and Spring Boot. The target architecture is a hybrid system:

- GraphQL for fast, repeatable listing discovery and incremental refresh
- PostgreSQL for normalized storage, history, and recovery state
- Playwright-based detail enrichment for fields not exposed cleanly through GraphQL
- Scheduled jobs for discovery, refresh, deletion detection, and downstream notifications

The initial production scope covers Real Estate, Transport, and Computers/Electronics subcategories on `999.md`. The system is designed to support full bootstrap crawls, incremental updates, price history, owner tracking, view tracking, detail enrichment, and operational recovery.

This document is the full plan for the project. It reflects the repository state as of March 12, 2026 and defines the path from the current codebase to a production-capable system.

## 2. Current Project Baseline

The repository already contains the platform foundation and should be treated as completed baseline work rather than future scope:

- Spring Boot `4.0.2` project under package `md.hashcode.vector9`
- Java 21, Maven, Actuator, Flyway, jOOQ runtime support, OkHttp, Testcontainers
- Profile-based configuration in `application.yml`, `application-dev.yml`, `application-prod.yml`, and `application-test.yml`
- Docker Compose local database stack with PostgreSQL `18-alpine`
- PowerShell database scripts for start, stop, reset, logs, and psql access
- `.env.example` and local environment ignores in `.gitignore`
- Custom application and database health indicators
- `DatabaseInfoService` and bootstrap/configuration test coverage

The main production work that remains is the actual scraping platform:

- database schema migrations for business entities
- jOOQ code generation
- GraphQL client and request/response models
- repository layer for crawler data
- crawler and scheduler services
- enrichment pipeline
- alerting, reporting, and production deployment hardening

## 3. Product Goals

The production system must:

- discover ads across configured subcategories
- keep an up-to-date index of active listings
- detect new ads, price changes, and deletions
- persist owners, images, and enrichment data
- recover safely after interruptions through checkpoints
- expose health and operational status for production monitoring
- support later downstream consumers such as Telegram alerts, reports, and analytics

## 4. Scope and Operating Model

### 4.1 Phase 1 Production Scope

Phase 1 delivers a reliable GraphQL-first crawler with database-backed recovery:

- subcategory seeding and enable/disable control
- full discovery crawl
- incremental updates every 15 minutes
- owner persistence
- image persistence
- price history
- deletion detection based on staleness
- view count collection
- health, logging, and operational metrics

### 4.2 Phase 2 Production Scope

Phase 2 adds selective HTML enrichment using Playwright:

- category-specific characteristics extraction
- geolocation and address extraction
- contact information extraction where available
- scheduled re-enrichment and retry flows

### 4.3 Future Scope

Future expansion can build on the same data model:

- Telegram notifications
- daily summary reports
- public/internal REST API
- dashboards and analytics
- trend analysis on views and pricing

## 5. Target Architecture

### 5.1 Collection Strategy

Vector9 should use a two-tier collection model:

- Tier 1: GraphQL listing crawl for scale, pagination, freshness, and most structured data
- Tier 2: Playwright detail crawl only when GraphQL is insufficient or when enrichment is required

### 5.2 Core GraphQL Operations

The crawler should standardize on these operations:

- `SearchAds` for paginated listing discovery and refresh
- `AdViews` for view statistics
- `AdSubcategoryUrl` for ad state, offer type, and category confirmation

Expected request posture:

- `source=desktop_redesign`
- `lang=ro`
- `includeOwner=true`
- `includeCarsFeatures=true` only for transport subcategories that need it
- `includeBody=false` for normal list crawling unless a use case justifies the larger payload

### 5.3 Detail Enrichment Strategy

Use Playwright only for ads that are:

- newly discovered
- price-changed
- not yet enriched
- scheduled for periodic refresh
- previously failed and eligible for retry

This keeps the high-volume crawl fast while still allowing complete structured data capture where needed.

## 6. Data Model

The production schema should include the following primary tables.

### 6.1 Core Tables

- `subcategories`
- `owners`
- `ads`
- `ad_images`
- `price_history`
- `crawl_checkpoints`

### 6.2 Enrichment and Analytics Tables

- `car_features`
- `ad_attributes`
- `view_history` if trend analytics are required in Phase 1; otherwise defer

### 6.3 Table Responsibilities

`subcategories`
- stores target subcategory metadata
- controls enablement and transport-specific feature toggles

`owners`
- stores stable seller identity and verification metadata

`ads`
- stores the main listing record, current price, state, timestamps, tracking fields, and enrichment status

`ad_images`
- stores image filenames and ordering

`price_history`
- stores every price transition worth tracking

`crawl_checkpoints`
- stores progress for resumable initial crawls

`car_features`
- stores normalized transport attributes returned from GraphQL or enrichment

`ad_attributes`
- stores flexible enrichment output such as characteristics, geolocation, and contact metadata

## 7. Initial Category Plan

The first production rollout should focus on the categories already emphasized in the prior plans:

- Real Estate
- Transport
- Computers and Electronics

Recommended initial enabled set:

- apartments, houses, villas, land
- cars, vans, motorcycles
- phones, laptops, desktops, mini PC, consoles, tablets, smart watches

The `subcategories` table should control enablement so rollout can expand gradually without redeploying code.

## 8. Application Structure

The existing package layout is appropriate and should be expanded rather than replaced:

- `config`
- `client`
- `crawler`
- `model`
- `repository`
- `service`
- `scheduler`
- `enrichment`
- `util`

Planned additions:

- GraphQL request builders and query constants in `client`
- DTOs and parsing models under `model/dto` and `model/graphql`
- jOOQ-backed repositories in `repository`
- orchestration services in `service`
- scheduled jobs in `scheduler`
- Playwright enrichment services in `enrichment`

## 9. Delivery Plan

### 9.1 Workstream A: Schema and Persistence

Deliverables:

- Flyway migrations for all production tables
- indexes for crawl, lookup, and cleanup queries
- seed migration for initial subcategories
- jOOQ code generation wired to the created schema
- repository interfaces and implementations for all core entities

Exit criteria:

- migrations apply cleanly on empty database
- tests can boot against migrated schema
- repository layer supports idempotent upsert flows

### 9.2 Workstream B: GraphQL Client

Deliverables:

- GraphQL query templates for `SearchAds`, `AdViews`, and `AdSubcategoryUrl`
- request builders with category-specific flags
- resilient OkHttp-based execution with timeouts and retry boundaries
- response parsing for ads, prices, images, owners, state, and counts

Exit criteria:

- mock-backed tests cover parsing and failure modes
- real integration smoke test can fetch one configured subcategory

### 9.3 Workstream C: Ad Processing

Deliverables:

- ad upsert logic
- owner upsert logic
- image refresh logic
- price change detection and price history writes
- timestamp normalization for `reseted` and crawl tracking fields

Exit criteria:

- repeated processing of the same ad is idempotent
- price deltas create exactly one history entry per change

### 9.4 Workstream D: Crawlers and Scheduling

Deliverables:

- initial discovery crawler with checkpoint recovery
- incremental crawler with stop-early logic on already-known ads
- deletion detection job using configurable staleness windows
- view count job for active ads

Exit criteria:

- initial crawl can resume after interruption
- incremental runs complete within the configured time budget
- stale ads transition cleanly to deleted state

### 9.5 Workstream E: Detail Enrichment

Deliverables:

- Playwright runtime setup
- detail scraping for category-specific characteristics
- structured JSON persistence to `ad_attributes`
- retry and backoff policy for failed enrichments

Exit criteria:

- new ads can be enriched asynchronously without blocking list crawling
- failed enrichments are visible and retryable

### 9.6 Workstream F: Operations

Deliverables:

- production logging policy
- health indicators for crawl freshness and dependency status
- metrics for requests, ads processed, failures, and latency
- Docker image and production deployment definition
- backup and restore runbook

Exit criteria:

- operators can detect crawler stalls and dependency failures quickly
- production deployment is reproducible and externally configurable

## 10. Crawl Logic

### 10.1 Initial Discovery

For each enabled subcategory:

1. load checkpoint or start at `skip=0`
2. call `SearchAds` with `limit=50`
3. persist returned ads and related records
4. update checkpoint
5. continue until `skip >= count`

### 10.2 Incremental Refresh

For each enabled subcategory:

1. fetch newest-first page
2. process ads in order
3. stop when reaching an already known ad with unchanged freshness marker
4. update `last_seen_at` for all encountered active ads

### 10.3 Deletion Detection

Because the API primarily returns active ads, missing ads should be inferred by staleness:

- default threshold: `7 days`
- configurable globally and, if needed later, per subcategory

### 10.4 View Tracking

Run in batches for active ads:

- fetch `today`, `total`, and `sinceRepublish`
- update current fields on `ads`
- optionally insert snapshots into `view_history`

## 11. Configuration Model

The existing configuration layout is sound and should be extended with production properties for:

- GraphQL endpoint and timeout
- crawl enablement and cron
- request delay and jitter
- page size
- deletion thresholds
- view tracking cadence
- enrichment cadence, batch size, and refresh policy

Recommended additions under `vector9`:

- `views`
- `deletion`
- `enrichment.retry`
- `subcategories.bootstrap`

## 12. Testing Strategy

### 12.1 Unit Tests

- GraphQL request building
- response parsing
- ad processing and price change logic
- deletion decision logic
- scheduler boundary behavior

### 12.2 Integration Tests

- Flyway migrations on PostgreSQL
- repository behavior against migrated schema
- GraphQL client against `MockWebServer`
- end-to-end processing of sample API payloads

### 12.3 Manual and Operational Tests

- one-subcategory real crawl smoke test
- database growth and checkpoint recovery test
- long-running incremental scheduling test
- Playwright enrichment verification against current site structure

## 13. Milestones

### Milestone 1: Persistence Foundation

- business schema exists
- jOOQ generation works
- repository layer is testable

### Milestone 2: GraphQL Crawl MVP

- one subcategory can be crawled end to end
- ads, owners, images, and price history persist correctly

### Milestone 3: Multi-Category Incremental Operation

- enabled subcategories run on schedule
- checkpoints and deletion detection work
- view collection works

### Milestone 4: Production Readiness

- logging, metrics, and health checks are complete
- Docker deployment is stable
- runbooks exist for startup, recovery, and DB maintenance

### Milestone 5: Detail Enrichment

- Playwright enrichment is running asynchronously
- structured attributes are available for target categories

## 14. Risks and Controls

### 14.1 Upstream API or Site Changes

Risk:
- GraphQL schema or page structure changes without notice

Controls:

- isolate query definitions
- validate responses defensively
- keep HTML enrichment decoupled from list crawling
- alert on failure-rate spikes

### 14.2 Rate Limiting or Blocking

Risk:
- request patterns trigger throttling

Controls:

- fixed delays plus jitter
- bounded concurrency
- category rollout in stages
- optional proxy strategy only if genuinely needed

### 14.3 Data Quality Drift

Risk:
- duplicate records, stale status, or partial owner/image updates

Controls:

- idempotent upserts
- transactional ad processing
- targeted uniqueness constraints
- regression tests using captured payloads

### 14.4 Operational Recovery Gaps

Risk:
- crawler restart loses position or creates inconsistent state

Controls:

- checkpoint persistence
- resumable jobs
- durable logs and health signals
- documented recovery procedures

## 15. Production Readiness Criteria

Vector9 is ready for production when all of the following are true:

- schema migrations and jOOQ generation are stable
- GraphQL crawling works across the initial enabled subcategories
- price changes, deletions, checkpoints, and view tracking are functioning
- operational metrics and health checks identify failures clearly
- deployment configuration is repeatable
- repository and crawler tests pass reliably in CI and local development

## 16. Immediate Next Steps

The next implementation sequence should be:

1. add Flyway migrations for the production schema
2. add jOOQ code generation after schema creation
3. implement the GraphQL client and models
4. implement repository and ad processing services
5. implement initial and incremental crawlers
6. add deletion and view tracking jobs
7. add production observability and deployment hardening
8. add Playwright enrichment as a separate workstream after the GraphQL crawler is stable
