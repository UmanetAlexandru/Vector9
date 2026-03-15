# Task 11: Downstream Notifications and Reporting

## Overview

Implement the first downstream consumer layer for Vector9 so the platform can turn detected crawl events into actionable operator-facing outputs. This task should build on the production crawl, deletion tracking, observability, and detail enrichment work from Tasks 08, 09, and 10. It should introduce a clean event-to-delivery pipeline for Telegram notifications and a bounded daily reporting flow without expanding into a full public API or analytics dashboard.

**Estimated Time**: 6-9 hours
**Priority**: High
**Dependencies**: Task 08 (Deletion and View Tracking Jobs), Task 09 (Production Observability and Deployment Hardening), Task 10 (Playwright Detail Enrichment)

---

## Objectives

1. Define the downstream event model for ads that are new, changed, deleted, or otherwise interesting
2. Add a notification delivery flow that can publish selected operational events to Telegram
3. Add a daily summary reporting flow delivered through Telegram
4. Keep delivery idempotent so repeated jobs do not send duplicate notifications unintentionally
5. Make downstream processing observable, retry-aware, and configurable

---

## Implementation Notes

- Keep downstream delivery code under `src/main/java/md/hashcode/vector9/service`, `scheduler`, `model`, or a dedicated `notification` package if that boundary is clearer
- Build on persisted data rather than trying to emit notifications inline from the crawler transaction path
- Treat notifications and reports as separate downstream consumers of the same durable event selection logic
- Prefer a generic notification abstraction so the first delivery channel can be added without hardwiring the rest of the application to Telegram forever
- Reuse existing observability patterns for job outcomes, delivery failures, and freshness where useful
- Avoid introducing a user-facing REST API or dashboard in this task

Confirmed decisions for this task:

- downstream notifications are now the next logical workstream after crawl, observability, and enrichment
- Telegram is the first required delivery target
- daily summary reporting should also be delivered through Telegram
- use a single fixed destination for all environments
- include the environment name in outbound messages, for example `[PROD]`
- immediate notifications should initially cover only errors or missing-status conditions rather than all business events
- future configurability expansion should be deferred to a follow-up improvement task

Recommended scope boundary for Task 11:

- include:
  - downstream event selection for operational alerts
  - notification channel abstraction
  - one concrete Telegram implementation
  - scheduled summary generation
  - delivery state tracking
- exclude:
  - public API endpoints
  - dashboard UI
  - advanced trend analytics
  - multi-tenant recipient management
  - configurable per-event subscriptions in the first version

---

## Implementation Steps

### Step 1: Define the Event and Delivery Model

Add a durable downstream model for events that are eligible for notification or reporting.

Recommended concepts:

- event types:
  - `JOB_FAILED`
  - `JOB_STALE`
  - `MISSING_STATUS`
  - `DAILY_SUMMARY`
- delivery status:
  - `PENDING`
  - `SENT`
  - `FAILED`
  - `SKIPPED`
- summary/report types:
  - `DAILY_SUMMARY`

Implementation options:

1. Add a dedicated notification/report delivery table through Flyway
2. Reuse existing job execution state plus deterministic queries if the schema already supports safe idempotency

Preferred direction:

- add explicit durable delivery tracking if current schema does not already provide a reliable deduplication key
- model each outbound notification candidate with:
  - event type
  - environment name
  - related job or subsystem name
  - optional ad id if an ad-specific failure is later included
  - event timestamp
  - payload fingerprint or natural dedupe key
  - delivery status
  - attempt count
  - last attempt timestamp
  - last error summary

### Step 2: Implement Event Selection

Create a service that queries persisted crawl state and yields downstream candidates.

Recommended initial candidate rules:

1. job executions that failed since the last successful downstream run
2. stale background jobs detected by freshness health logic
3. missing-status conditions where an expected job execution record or state marker is absent
4. optionally enrichment failures above a threshold if operator visibility is needed

Requirements:

- event selection should be repository-driven and deterministic
- repeated runs must not create duplicate outbound events for the same state change
- event selection should operate independently from the crawler job transaction
- per-event-type enablement should be configurable

### Step 3: Add Notification Formatting

Create a formatter layer that maps operational events into human-readable outbound messages.

Recommended message content:

- environment marker such as `[DEV]` or `[PROD]`
- job or subsystem name
- failure or stale-status summary
- relevant timestamps
- short diagnostic context
- optional next-action hint where useful

Requirements:

- formatting should be channel-aware but mostly reusable
- do not leak raw internal payloads
- keep message length bounded for chat delivery
- include stable links back to the ad when available

### Step 4: Add a Notification Channel Abstraction

Define a small interface for outbound delivery, for example:

- `NotificationChannel`
- `NotificationMessage`
- `NotificationDeliveryResult`

Recommended first implementation:

- Telegram bot delivery using externally configured bot token and chat id

Requirements:

- channel credentials must come from environment-driven configuration
- delivery errors must be captured without crashing the whole batch
- one failed notification must not block unrelated deliveries
- the implementation should allow additional channels later, such as email or webhooks

### Step 5: Add Reporting Flow

Implement a bounded reporting service that generates scheduled summaries from persisted operational and crawl data.

Recommended initial report contents:

- job runs by type
- job failures by type
- stale or missing-status incidents detected in the window
- optional crawl activity counts such as ads processed, deleted, or enriched if those metrics are already available cheaply

Recommended delivery options:

- send the report through the same first notification channel
- optionally persist a rendered report snapshot if operational history is useful

Requirements:

- report generation must be time-window based
- schedule cadence should be configurable
- summary queries should stay efficient for the expected production dataset size

### Step 6: Add Scheduler-Ready Entry Points

Keep notification and reporting orchestration invokable directly and ready for cron wiring.

Suggested class names:

- `DownstreamEventService`
- `NotificationDispatchService`
- `ReportGenerationService`
- `TelegramNotificationChannel`
- `DownstreamJobResult`

Recommended jobs:

- near-real-time operational notification dispatch job
- daily summary report job

### Step 7: Add Observability and Failure Handling

Extend the existing operations layer with downstream delivery signals.

Requirements:

- metrics for events selected, notifications sent, failures, retries, and report generation duration
- logs with event type, ad id, and channel context
- health or freshness visibility if downstream delivery becomes required for production confidence
- configurable retry limit and backoff for failed deliveries

### Step 8: Add a Manual Smoke Path

Add a disabled manual smoke test or dev-only entry point that:

- selects one recent event
- renders one outbound message
- optionally sends it to a configured non-production target
- stays outside the automated suite by default

---

## Suggested Class Set

The exact names can vary, but this task should likely produce classes close to:

- `DownstreamEventSelector`
- `NotificationDispatchService`
- `NotificationFormatter`
- `NotificationChannel`
- `TelegramNotificationChannel`
- `ReportGenerationService`
- `DownstreamProperties`
- `DownstreamJobResult`

Potential minimal extensions to existing code:

- repository queries for failed or stale job states
- Flyway migration for delivery tracking if needed
- scheduler classes for dispatch and reporting jobs
- observability hooks for delivery metrics

---

## Behavioral Requirements

### Event Selection

- failed jobs should be detectable exactly once for notification purposes
- stale job conditions should generate one downstream event per stale incident window rather than spamming repeats
- missing-status conditions should be eligible once until the condition clears or materially changes
- repeated downstream runs should be idempotent

### Delivery

- one failed outbound call should not abort the whole batch
- retries should be bounded and visible
- successful delivery should mark the event as sent durably
- message formatting should remain stable and readable even with partial diagnostic context

### Reporting

- reports should summarize a clear fixed time window
- repeated report runs for the same window should not duplicate delivery unintentionally
- summary generation should remain useful even when some optional enrichment data is absent

### Recovery and Operations

- delivery jobs should be restart-safe
- failure reasons should be inspectable without digging through raw payload dumps
- downstream metrics should make silent delivery failure visible

---

## Testing Strategy

### Unit and Service Tests

Add focused tests for:

- event selection for new ads, price changes, and deletions
- event selection for failed jobs, stale jobs, and missing-status conditions
- deduplication and idempotency behavior
- notification formatting for each supported event type
- retry handling and failure classification
- report generation across a bounded time window

Recommended test locations:

```text
src/test/java/md/hashcode/vector9/service/
src/test/java/md/hashcode/vector9/scheduler/
src/test/java/md/hashcode/vector9/notification/
```

Use mocks or fixtures for:

- external notification channel calls
- repository collaborators not under direct test
- clock/time window boundaries

### Integration Tests

Add coverage for:

- repository-backed event selection and delivery state persistence
- application startup with downstream configuration enabled
- one mock-backed outbound channel integration path

### Narrow Validation While Iterating

Run focused tests first:

```powershell
mvn -Dtest=NotificationDispatchServiceTest test
mvn -Dtest=ReportGenerationServiceTest test
```

### Full Validation Before Commit

Run:

```powershell
mvn test
```

Expected result:

- downstream tests pass
- existing crawler, repository, observability, and enrichment tests still pass
- no regression in application startup or scheduler wiring

---

## Checklist

- [ ] Task 11 task file created
- [ ] downstream event model defined
- [ ] notification delivery abstraction added
- [ ] first concrete notification channel implemented
- [ ] reporting flow implemented
- [ ] delivery tracking and retry handling implemented
- [ ] disabled manual smoke path added
- [ ] focused tests added
- [ ] full test suite passes

---

## Deliverables

1. Durable downstream event selection for operational notifications and reports
2. Configurable outbound notification flow with Telegram as the first concrete channel
3. Daily summary reporting over persisted operational and crawl data
4. Tests plus one disabled manual downstream smoke path

---

## Next Steps

After completing this task:

1. add more delivery channels if needed, such as email or generic webhooks
2. expand notifications beyond operational alerts into business-event delivery if needed
3. add a public or internal API only if downstream consumers outgrow message-based delivery

---

## Open Questions

1. The current plan does not specify whether downstream delivery needs a new durable table or can reuse existing business tables plus job timestamps.
   If strict idempotent delivery is required, a dedicated delivery tracking table is the safer implementation.

2. A Telegram username is not sufficient for Bot API delivery.
   Implementation still needs the numeric Telegram `chat_id` for the target conversation.

3. "Missing statuses" needs to be interpreted concretely in code.
   The recommended first meaning is missing or stale job execution state for expected scheduler-driven processes.

---

**Task Status**: Ready For Implementation Pending Telegram Chat ID
**Estimated Completion**: 6-9 hours
**Blocker For**: External Notifications, Scheduled Reports, and Broader Downstream Consumption
