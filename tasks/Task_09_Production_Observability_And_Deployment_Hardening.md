# Task 09: Production Observability and Deployment Hardening

## Overview

Implement the production observability and deployment hardening layer for Vector9 so operators can detect crawl failures quickly, understand system freshness, and run the application in a reproducible production environment. This task should build on the crawler, deletion, and view tracking functionality from Tasks 07 and 08. It should not yet implement Playwright enrichment or downstream alert delivery such as Telegram notifications.

**Estimated Time**: 5-7 hours
**Priority**: High
**Dependencies**: Task 01 (Project Setup), Task 07 (Crawler Orchestration), Task 08 (Deletion and View Tracking Jobs)

---

## Objectives

1. Add health indicators for crawler freshness and key dependencies
2. Add metrics for GraphQL requests, ads processed, failures, and job execution outcomes
3. Define a production logging policy that supports troubleshooting and operations
4. Add Docker and runtime configuration needed for reproducible production deployment
5. Document the operational workflow for startup, recovery, and basic maintenance

---

## Implementation Notes

- Keep production observability code under `src/main/java/md/hashcode/vector9/config`, `service`, or `util`
- Reuse existing Actuator integration, health endpoint exposure, and database health wiring
- Keep metric naming explicit and consistent so later dashboards are easy to build
- Avoid introducing a full alerting stack in this task; focus on signals and deployment readiness
- Treat deployment hardening as configuration, containerization, and runbook work, not orchestration platform-specific automation
- Prefer externally configurable properties for thresholds, file paths, and job freshness windows
- Do not implement Playwright runtime or enrichment scheduling here

Assumptions carried from the production plan:

- operators need to detect crawler stalls and dependency failures quickly
- deployment should be reproducible through Docker image and environment-driven configuration
- job freshness is more important operationally than fine-grained business reporting at this stage
- metrics should cover request counts, ads processed, failures, and latency at minimum

---

## Implementation Steps

### Step 1: Add Observability Configuration

Expand or add configuration for:

```text
src/main/java/md/hashcode/vector9/config/
src/main/resources/
```

Recommended additions:

- properties for freshness thresholds
- metric naming or tagging helpers if needed
- production-focused logging configuration

Suggested property areas:

- `vector9.observability`
- `vector9.health`
- `vector9.logging`

### Step 2: Add Crawl Freshness Health Signals

Implement health checks that answer questions such as:

1. has incremental crawling run recently enough
2. has deletion detection run recently enough
3. has view tracking run recently enough
4. are core dependencies such as the database and GraphQL endpoint reachable enough for production use

Requirements:

- health output should distinguish healthy, stale, and failed states clearly
- thresholds should be configurable
- health checks should include enough context for operations without exposing sensitive values
- existing database health behavior should remain intact

### Step 3: Add Metrics

Add metrics for at least:

- GraphQL request count
- GraphQL request failures
- GraphQL request latency
- ads processed per crawl
- stale ads marked deleted
- view tracking batches attempted/succeeded/failed
- job execution duration

Requirements:

- metrics should use Actuator/Micrometer primitives already available through Spring Boot
- labels/tags should stay low-cardinality
- metrics should be usable for later dashboards or alert thresholds

### Step 4: Define Logging Policy

Harden logging for production use.

Requirements:

- keep structured and readable console logging
- ensure failures include enough job and subcategory context
- avoid over-logging large payloads by default
- make file logging path externally configurable
- define log-level expectations for:
  - normal operations
  - retryable failures
  - hard failures

### Step 5: Add Deployment Hardening

Add the core pieces needed for reproducible production deployment.

Possible deliverables:

- production-ready Dockerfile
- environment variable documentation
- container startup defaults
- healthcheck integration for the container image
- production profile adjustments

Requirements:

- runtime configuration must come from environment or mounted config
- secrets must not be hardcoded
- image startup should fail fast on invalid required configuration

### Step 6: Add Runbook Documentation

Document the minimum operator workflow for:

1. starting the service
2. verifying health and freshness
3. checking logs
4. validating DB connectivity
5. recovering from a failed or stalled crawl

Recommended locations:

- `README.md`
- `tasks/`
- `docs/` if added by the implementation

### Step 7: Keep Future Alerting Boundaries Clear

For this task:

- expose the signals needed for alerting later
- do not wire external notification channels yet
- keep future integrations straightforward by returning clear health and metric signals now

---

## Suggested Class Set

The exact names can vary, but this task should likely produce classes close to:

- `CrawlerFreshnessHealthIndicator`
- `JobFreshnessService`
- `GraphqlMetricsService`
- `OperationalMetricsRecorder`
- `ProductionRuntimeProperties`

Potential minimal extensions to existing code:

- persist last-success timestamps for background jobs if they are not already available
- add lightweight execution tracking around crawler, deletion, and view services
- extend existing health indicators rather than replacing them

---

## Behavioral Requirements

### Health and Freshness

- stale background jobs should be visible through health endpoints
- dependency failures should be surfaced clearly
- healthy status should include enough evidence to trust it operationally

### Metrics

- normal job execution should emit success and duration metrics
- failures should emit failure counts without requiring log scraping
- request latency should be measurable for upstream GraphQL calls

### Logging

- logs should support tracing a failed job without dumping unnecessary payload data
- repeated transient failures should remain readable and actionable
- normal operation logs should not overwhelm production storage

### Deployment

- the application should run cleanly in a production profile with external configuration
- production startup should be reproducible from documented inputs
- operators should not need repository-local assumptions to deploy the service

---

## Testing Strategy

### Unit Tests

Add focused tests for:

- health indicator stale vs healthy transitions
- metric recording on success and failure paths
- logging or configuration defaults where practical
- production property binding for new observability settings

### Integration Tests

Add coverage for:

- actuator health endpoint containing the new health signals
- application startup with the production-related configuration additions
- container or runtime config validation where feasible

Recommended test locations:

```text
src/test/java/md/hashcode/vector9/config/
src/test/java/md/hashcode/vector9/integration/
```

### Narrow Validation While Iterating

Run focused tests first:

```powershell
mvn -Dtest=CrawlerFreshnessHealthIndicatorTest test
mvn -Dtest=ApplicationIntegrationTest test
```

### Full Validation Before Commit

Run:

```powershell
mvn test
```

Expected result:

- new observability tests pass
- existing crawler, repository, and application tests still pass
- no regression in health endpoint behavior

---

## Checklist

- [ ] Task 09 task file created
- [ ] freshness health indicators added
- [ ] metrics for requests and jobs added
- [ ] production logging policy implemented
- [ ] deployment/runtime hardening added
- [ ] operator runbook documentation added
- [ ] focused tests added
- [ ] full test suite passes

---

## Deliverables

1. Production health indicators for crawler freshness and dependencies
2. Metrics for GraphQL requests and background job outcomes
3. Hardened logging and production runtime configuration
4. Reproducible deployment assets and operator runbook updates

---

## Next Steps

After completing this task:

1. implement Playwright enrichment as the next major collection workstream
2. add downstream alerting and notification integrations on top of the observability signals
3. expand dashboards or reports once production metrics stabilize

---

## Open Questions

1. The production plan calls for Docker deployment definition, but does not mandate a specific target platform.
   Task 09 should stay platform-neutral unless you want to standardize on a specific deployment target later.

2. Health freshness requires a source of truth for last successful job execution.
   If existing services do not yet persist that state, Task 09 should add the lightest durable mechanism that makes health signals trustworthy.

---

**Task Status**: Ready for Implementation
**Estimated Completion**: 5-7 hours
**Blocker For**: Production Readiness and Enrichment Rollout
