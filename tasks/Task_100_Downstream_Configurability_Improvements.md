# Task 100: Downstream Configurability Improvements

## Overview

Add the second-stage flexibility layer for downstream delivery after the first Telegram-based operational notification flow is stable in production. This task should expand routing, event selection, and delivery policy without rewriting the core downstream pipeline introduced in Task 11.

**Estimated Time**: 4-6 hours
**Priority**: Medium
**Dependencies**: Task 11 (Downstream Notifications and Reporting)

---

## Objectives

1. Make downstream event selection configurable by event type
2. Support different destinations by environment and delivery purpose
3. Allow business-event notifications to be enabled later without structural rewrites
4. Keep configuration externalized and safe to change without code edits

---

## Implementation Notes

- Build on the Task 11 notification abstraction, not around it
- Keep defaults safe and low-noise
- Prefer explicit property binding over ad hoc environment-variable parsing
- Do not introduce end-user subscription management unless it becomes a real requirement

---

## Suggested Improvements

### Event Selection Config

- enable or disable individual event types such as:
  - `JOB_FAILED`
  - `JOB_STALE`
  - `MISSING_STATUS`
  - `NEW_AD`
  - `PRICE_CHANGED`
  - `AD_DELETED`
  - `ENRICHMENT_FAILED`

### Destination Config

- different Telegram chat ids for `dev`, `stage`, and `prod`
- separate destinations for:
  - operational alerts
  - daily summaries
  - business-event notifications

### Delivery Policy Config

- quiet hours or summary-only windows
- per-event retry policy
- batching thresholds
- duplicate suppression windows

### Message Formatting Config

- configurable environment prefixes
- configurable message templates or sections
- optional inclusion of diagnostic details

---

## Deliverables

1. Expanded `DownstreamProperties` model
2. Configurable event-to-destination routing
3. Configurable event enablement and delivery policies
4. Tests covering the new configuration combinations

---

## Open Questions

1. Whether downstream routing should stay static by environment or become data-driven later
2. Whether business-event notifications should share the same channel as operational alerts
3. Whether report delivery should eventually be split from alert delivery

---

**Task Status**: Deferred Improvement Task
**Estimated Completion**: 4-6 hours
