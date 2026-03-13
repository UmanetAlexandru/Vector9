# Task 06: GraphQL Client and Request/Response Models

## Overview

Implement the GraphQL client layer for Vector9 so the application can fetch listing data from `999.md` through typed request builders and parsed response models. This task should stop at transport, query construction, and response parsing. It should not yet implement crawl loops or scheduling.

**Estimated Time**: 4-6 hours
**Priority**: Critical
**Dependencies**: Task 05 (Repository Layer and Ad Processing Services)

---

## Objectives

1. Add a resilient GraphQL HTTP client on top of OkHttp
2. Define request builders for the core GraphQL operations
3. Add response models for listing, owner, image, and view payloads
4. Parse GraphQL responses into typed Java models
5. Verify client behavior with `MockWebServer` tests

---

## Implementation Notes

- Keep GraphQL transport code under `src/main/java/md/hashcode/vector9/client`
- Keep GraphQL DTOs under a dedicated package such as `model/graphql`
- Use the existing `OkHttpClient` bean from the config layer
- Keep this task focused on transport and parsing only
- Do not call repositories directly from the GraphQL client
- Do not implement scheduler logic or crawl orchestration in this task
- Keep request construction explicit so category-specific flags are easy to change later

Expected initial operations:

- `SearchAds`
- `AdViews`
- `AdSubcategoryUrl`

Expected request posture from the captured mini-PC page:

- HTTP method `POST`
- endpoint `https://999.md/graphql`
- header `content-type=application/json`
- header `lang=ro`
- header `source=desktop`
- `lang=ro`
- `includeOwner=true`
- `includeCarsFeatures=false` by default, enabled only for transport categories
- `includeBody=false` for normal listing crawls
- `includeBoost=false` by default for normal listing crawls
- GraphQL input source `AD_SOURCE_DESKTOP`

Observed category and filter mapping for the captured page:

- page URL: `/ro/list/computers-and-office-equipment/mini-pc?o_16_1=776`
- category id: `2`
- subcategory slug: `computers-and-office-equipment/mini-pc`
- subcategory id: `7661`
- filter mapping: `filterId=16`, `featureId=1`, `optionIds=[776]`

Observed `SearchAds` request shape for this page:

```json
{
  "operationName": "SearchAds",
  "variables": {
    "isWorkCategory": false,
    "includeCarsFeatures": false,
    "includeBody": false,
    "includeOwner": true,
    "includeBoost": false,
    "input": {
      "subCategoryId": 7661,
      "source": "AD_SOURCE_DESKTOP",
      "filters": [
        {
          "filterId": 16,
          "features": [
            {
              "featureId": 1,
              "optionIds": [776]
            }
          ]
        }
      ],
      "pagination": {
        "limit": 78,
        "skip": 0
      }
    },
    "locale": "ro_RO"
  }
}
```

Observed count-only request variant:

- same operation and filters
- `pagination.limit=0`
- returns `ads=[]` and a valid `count`

Assumption for uncaptured operations:

- implement `AdViews` and `AdSubcategoryUrl` in the client layer as first-class operations now
- keep their request/response models minimal and extensible
- validate transport, serialization, and error handling with tests even if their exact upstream field set was not captured on this page
- do not block Task 06 on obtaining a perfect live contract for those two operations

---

## Implementation Steps

### Step 1: Add GraphQL Client Package Structure

Create or expand:

```text
src/main/java/md/hashcode/vector9/client/
src/main/java/md/hashcode/vector9/model/graphql/
```

Recommended class split:

- low-level HTTP client
- request/query builder classes
- response wrapper models
- mapping/parsing helpers

### Step 2: Implement Core GraphQL Queries

Add query definitions for:

- `SearchAds`
- `AdViews`
- `AdSubcategoryUrl`

Keep the query text centralized, for example in:

- `GraphqlQueries`
- `GraphqlOperation`

Requirements:

- query names should match the upstream operation names
- variables must be passed separately from the query body
- request serialization should be deterministic for testing
- `SearchAds` query text should match the captured contract closely enough to parse:
  - `id`
  - `title`
  - `subCategory`
  - `price`
  - `pricePerMeter`
  - `oldPrice`
  - `images`
  - `owner`
  - `transportYear`
  - `realEstate`
  - `label`
  - `frame`
  - `animation`
  - `animationAndFrame`
  - ad-level `reseted`
  - response-level `count` and `reseted`

### Step 3: Add Request Models and Builders

Introduce request models that can build the JSON body sent to the GraphQL endpoint.

Recommended examples:

- `SearchAdsRequest`
- `AdViewsRequest`
- `AdSubcategoryUrlRequest`
- `GraphqlRequest<TVariables>`

The `SearchAds` builder should support:

- subcategory id
- pagination (`limit`, `skip`)
- sort order
- language
- owner/body/cars-features flags
- boost flag
- source value
- arbitrary filter groups expressed as upstream `filterId` and `featureId` pairs with `optionIds`

Builder defaults for normal listing crawls:

- `includeOwner=true`
- `includeBody=false`
- `includeCarsFeatures=false`
- `includeBoost=false`
- `isWorkCategory=false`
- `locale=ro_RO`
- `input.source=AD_SOURCE_DESKTOP`

### Step 4: Add Response Models

Add response models for the GraphQL data actually needed by later tasks.

The model set should cover at least:

- ads list and total count
- owner identity and metadata
- image filenames/ordering
- price fields
- ad state / offer type fields
- view counts

Recommended examples:

- `GraphqlResponse<T>`
- `GraphqlError`
- `SearchAdsResponse`
- `SearchAdsData`
- `GraphqlAd`
- `GraphqlOwner`
- `GraphqlImage`
- `AdViewsResponse`
- `AdSubcategoryUrlResponse`

Concrete `SearchAds` response details observed in the mini-PC capture:

- top-level:
  - `data.searchAds.ads`
  - `data.searchAds.count`
  - `data.searchAds.reseted`
- per ad:
  - `id`
  - `title`
  - `subCategory.id`
  - `subCategory.title.translated`
  - `subCategory.parent`
  - `price`
  - `pricePerMeter`
  - `oldPrice`
  - `images`
  - `owner` when requested
  - `transportYear`
  - `realEstate`
  - `label`
  - `frame`
  - `animation`
  - `animationAndFrame`
  - `reseted`

Important upstream value shapes:

- `price` is a `FeatureValue` with nested object value:
  - `bargain`
  - `down_payment`
  - `measurement`
  - `mode`
  - `unit`
  - `value`
- `oldPrice` may be a simple numeric feature value
- `images.value` is a string array
- image entries are inconsistent:
  - plain filenames such as `abc.jpg`
  - filenames with query metadata such as `abc.jpg?metadata=...`
- `owner.business` and `owner.verification` are nullable
- `label`, `frame`, `animation`, `animationAndFrame` are nullable
- top-level `searchAds.reseted` is numeric in the capture
- ad-level `reseted` is formatted text in the capture
- preserve all of these nullable or polymorphic edges without lossy coercion

Owner fields to support when `includeOwner=true`:

- `owner.id`
- `owner.login`
- `owner.avatar`
- `owner.createdDate`
- `owner.business.plan`
- `owner.business.id`
- `owner.verification.isVerified`
- `owner.verification.date`

Display fields worth modeling now because they already appear in captured list-like responses:

- `label.enable`
- `label.title`
- `label.color.{r,g,b,a}`
- `label.gradient.{from,to,position,rotation}`
- `frame.enable`
- `animation.enable`
- `animationAndFrame.enable`

### Step 5: Implement Client Execution and Parsing

Add a `GraphqlClient` service that:

- serializes the request body as JSON
- executes POST requests to the configured GraphQL endpoint
- enforces timeouts through the configured OkHttp client
- validates HTTP status
- parses successful responses into typed models
- surfaces GraphQL errors clearly

Requirements:

- non-2xx HTTP responses should fail explicitly
- GraphQL `errors` responses should not be silently ignored
- malformed JSON should fail with a clear exception

### Step 6: Keep Mapping Boundaries Clean

This task may add small helper mappers if needed, but avoid binding the client directly to persistence commands.

The output of this task should be reusable by later crawler services.

---

## Suggested Class Set

The exact names can vary, but the task should likely produce classes close to:

- `GraphqlClient`
- `GraphqlQueries`
- `GraphqlRequest`
- `GraphqlResponse`
- `GraphqlError`
- `SearchAdsRequest`
- `AdViewsRequest`
- `AdSubcategoryUrlRequest`
- `SearchAdsResponse`
- `GraphqlAd`
- `GraphqlOwner`
- `GraphqlImage`

---

## Behavioral Requirements

### Transport

- all requests must use POST
- content type must be JSON
- GraphQL endpoint must come from configuration
- request and response handling must be testable without live network access

### SearchAds Parsing

- parse total result count
- parse ad ids, titles, prices, owners, and images
- preserve optional fields as nullable where upstream data is inconsistent
- preserve currency and pricing metadata, not only numeric value
- preserve category breadcrumb data from `subCategory.parent`
- do not assume image values are bare filenames
- do not assume seller-type-like data is a dedicated owner field; on this page it appears under `realEstate: feature(id=795)`

### Error Handling

- distinguish HTTP transport failure from GraphQL semantic failure
- expose operation name in error messages where practical
- include enough context for later retry/logging decisions

### Extensibility

- query and model structure should support later additions without rewriting the client core
- feature values should be modeled in a way that can tolerate mixed primitive and object payloads
- requests should support additional filter groups without changing the client transport layer

---

## Testing Strategy

### Unit Tests

Add focused tests for:

- request JSON serialization
- operation-specific builder defaults
- error parsing
- `SearchAds` default builder values with `includeOwner=true`
- filter serialization for `filterId=16`, `featureId=1`, `optionIds=[776]`
- mixed `FeatureValue.value` parsing for:
  - structured price objects
  - numeric old prices
  - string-array image payloads

### Integration Tests

Use `MockWebServer` to verify:

- POST request shape and headers
- correct endpoint path usage
- parsing of successful `SearchAds` responses
- parsing of `AdViews` responses
- handling of GraphQL `errors`
- handling of non-200 HTTP responses
- parsing of nullable owner/display fields
- parsing of count-only `SearchAds` responses with `limit=0`

Recommended test locations:

```text
src/test/java/md/hashcode/vector9/client/
src/test/java/md/hashcode/vector9/integration/
```

### Full Validation Before Commit

Run:

```powershell
mvn test
```

Expected result:

- GraphQL client tests pass
- existing repository and schema tests still pass
- no regression in application startup tests

---

## Checklist

- [ ] Task 06 task file created
- [ ] GraphQL client package structure added
- [ ] query constants or definitions added
- [ ] request models/builders implemented
- [ ] response models implemented
- [ ] typed GraphQL client implemented
- [ ] `MockWebServer` coverage added for success and failure paths
- [ ] full test suite passes
- [ ] `SearchAds` defaults aligned to `includeOwner=true`
- [ ] mini-PC captured filter contract documented and covered by tests

---

## Deliverables

1. Typed GraphQL client layer
2. Request builders for `SearchAds`, `AdViews`, and `AdSubcategoryUrl`
3. Response models for ads, owners, images, views, and errors
4. Automated tests proving request and parsing behavior

---

## Next Steps

After completing this task:

1. map GraphQL listing responses into persistence commands
2. implement initial discovery and incremental crawler orchestration
3. connect checkpoint updates and ad persistence to the GraphQL fetch flow

---

**Task Status**: Ready for Implementation
**Estimated Completion**: 4-6 hours
**Blocker For**: Crawler Orchestration Tasks
