# Task 03: Database Schema and Flyway Migrations

## Overview

Create the production database schema for Vector9 using Flyway migrations. This task establishes the core crawler persistence model so later tasks can implement jOOQ code generation, repositories, GraphQL ingestion, and scheduled crawling on top of a stable schema.

**Estimated Time**: 3-5 hours
**Priority**: Critical
**Dependencies**: Task 02 (Database Setup)

---

## Objectives

1. Create Flyway migrations for the production crawler schema
2. Add the initial database tables, keys, and indexes required by the production plan
3. Seed a single initial subcategory for controlled rollout
4. Ensure migrations run cleanly on empty databases and automated test databases
5. Prepare the schema for Task 04 jOOQ code generation without enabling it yet

---

## Implementation Notes

- This task should define the schema only; do not implement repositories or crawler services yet
- Use Flyway SQL migrations under `src/main/resources/db/migration`
- Keep the schema aligned with the current `tasks/Vector9_Production_Plan.md`
- Preserve owner UUID as the primary key
- Do not add `owner_login` to `ads` in this task
- Keep transaction-boundary decisions for owner/ad writes out of the schema; those belong to service-layer design later
- Seed only one initial subcategory for controlled rollout
- Recommended initial seed: `6678` villas
- Do not enable jOOQ code generation in this task; Task 04 should do that after the schema is in place

---

## Implementation Steps

### Step 1: Create Flyway Migration Directory Structure

Use the existing Flyway location:

```text
src/main/resources/db/migration/
```

Add versioned migration files for:

- base crawler schema
- initial subcategory seed data

Recommended initial file layout:

```text
src/main/resources/db/migration/
|-- V1__create_vector9_schema.sql
`-- V2__seed_initial_subcategory.sql
```

If the repository already contains earlier baseline migrations, continue numbering from the next available version instead of reusing `V1` and `V2`.

### Step 2: Create Core Schema Migration

Create a migration for the core tables defined in the production plan:

- `subcategories`
- `owners`
- `ads`
- `ad_images`
- `price_history`
- `crawl_checkpoints`
- `car_features`
- `ad_attributes`
- `view_history`

#### `subcategories`

```sql
CREATE TABLE subcategories (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    parent_category_id BIGINT,
    parent_category_name VARCHAR(255),
    enabled BOOLEAN DEFAULT true,
    include_cars_features BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subcategories_enabled ON subcategories(enabled);
CREATE INDEX idx_subcategories_parent ON subcategories(parent_category_id);
```

#### `owners`

```sql
CREATE TABLE owners (
    id UUID PRIMARY KEY,
    login VARCHAR(100) UNIQUE NOT NULL,
    avatar VARCHAR(255),
    created_date VARCHAR(50),
    business_plan VARCHAR(50),
    business_id VARCHAR(50),
    is_verified BOOLEAN DEFAULT false,
    verification_date VARCHAR(50),
    first_seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_owners_login ON owners(login);
CREATE INDEX idx_owners_verified ON owners(is_verified);
```

#### `ads`

```sql
CREATE TABLE ads (
    id BIGINT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    subcategory_id BIGINT NOT NULL REFERENCES subcategories(id),

    price_value DECIMAL(12,2),
    price_unit VARCHAR(10),
    price_measurement VARCHAR(50),
    price_mode VARCHAR(50),
    price_per_meter DECIMAL(12,2),
    old_price_value DECIMAL(12,2),

    body_ro TEXT,
    body_ru TEXT,

    ad_state VARCHAR(50),
    offer_type_id INTEGER,
    offer_type_value INTEGER,
    offer_type_text VARCHAR(100),

    owner_id UUID REFERENCES owners(id),
    transport_year INTEGER,
    real_estate_type VARCHAR(100),

    status VARCHAR(20) DEFAULT 'active',
    last_seen_at TIMESTAMP,
    last_updated_at TIMESTAMP,
    first_seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    details_enriched BOOLEAN DEFAULT false,
    enrichment_status VARCHAR(50) DEFAULT 'pending',
    enrichment_attempts INTEGER DEFAULT 0,
    enrichment_last_attempt_at TIMESTAMP,
    details_last_enriched_at TIMESTAMP,

    views_today INTEGER,
    views_total INTEGER,
    views_since_republish INTEGER,
    views_last_fetched_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ads_subcategory ON ads(subcategory_id);
CREATE INDEX idx_ads_status ON ads(status);
CREATE INDEX idx_ads_last_updated ON ads(last_updated_at);
CREATE INDEX idx_ads_owner ON ads(owner_id);
CREATE INDEX idx_ads_details_enriched ON ads(details_enriched);
CREATE INDEX idx_ads_enrichment_status ON ads(enrichment_status);
CREATE INDEX idx_ads_ad_state ON ads(ad_state);
```

#### `ad_images`

```sql
CREATE TABLE ad_images (
    id BIGSERIAL PRIMARY KEY,
    ad_id BIGINT NOT NULL REFERENCES ads(id) ON DELETE CASCADE,
    image_filename VARCHAR(255) NOT NULL,
    position INTEGER NOT NULL,
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ad_id, position)
);

CREATE INDEX idx_ad_images_ad_id ON ad_images(ad_id);
CREATE UNIQUE INDEX idx_ad_images_ad_id_filename ON ad_images(ad_id, image_filename);
```

Notes:

- keep old images
- insert only new images
- deduplicate by `(ad_id, image_filename)`

#### `price_history`

```sql
CREATE TABLE price_history (
    id BIGSERIAL PRIMARY KEY,
    ad_id BIGINT NOT NULL REFERENCES ads(id) ON DELETE CASCADE,
    old_price DECIMAL(12,2),
    new_price DECIMAL(12,2),
    price_unit VARCHAR(10),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_price_history_ad ON price_history(ad_id);
CREATE INDEX idx_price_history_date ON price_history(changed_at);
```

#### `crawl_checkpoints`

```sql
CREATE TABLE crawl_checkpoints (
    subcategory_id BIGINT PRIMARY KEY REFERENCES subcategories(id),
    current_skip INTEGER DEFAULT 0,
    last_checkpoint_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_ads_count INTEGER,
    ads_processed INTEGER DEFAULT 0
);
```

#### `car_features`

```sql
CREATE TABLE car_features (
    ad_id BIGINT PRIMARY KEY REFERENCES ads(id) ON DELETE CASCADE,
    fuel_type VARCHAR(50),
    drive_type VARCHAR(50),
    transmission VARCHAR(50),
    mileage INTEGER,
    engine_volume DECIMAL(4,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### `ad_attributes`

```sql
CREATE TABLE ad_attributes (
    ad_id BIGINT PRIMARY KEY REFERENCES ads(id) ON DELETE CASCADE,
    characteristics JSONB NOT NULL,
    location JSONB,
    contact_info JSONB,
    enriched_at TIMESTAMP NOT NULL,
    scrape_duration_ms INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### `view_history`

```sql
CREATE TABLE view_history (
    id BIGSERIAL PRIMARY KEY,
    ad_id BIGINT NOT NULL REFERENCES ads(id) ON DELETE CASCADE,
    views_total INTEGER NOT NULL,
    views_since_republish INTEGER NOT NULL,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_view_history_ad ON view_history(ad_id);
CREATE INDEX idx_view_history_recorded_at ON view_history(recorded_at);
```

### Step 3: Create Initial Seed Migration

Add a second migration that seeds one initial subcategory for the controlled rollout strategy.

Recommended seed:

```sql
INSERT INTO subcategories (
    id,
    name,
    parent_category_id,
    parent_category_name,
    enabled,
    include_cars_features
) VALUES (
    6678,
    'Villas',
    270,
    'Real Estate',
    true,
    false
);
```

Notes:

- seed only one subcategory in this task
- keep other categories for later migrations or manual enablement
- use `enabled=true` only for the one initial rollout target

### Step 4: Add Schema Constraints and Defaults Carefully

Requirements:

- use foreign keys where identity is stable and known
- use `ON DELETE CASCADE` only where child rows are truly dependent
- prefer explicit indexes for crawler access patterns
- keep status fields as plain strings for now; do not introduce PostgreSQL enum types unless there is a clear operational benefit

Do not add in this task:

- database triggers for `updated_at`
- stored procedures
- materialized views
- partitioning
- custom enum types

These can be introduced later if operational evidence justifies them.

### Step 5: Verify Test Compatibility

The schema must work with:

- local PostgreSQL started through Docker Compose
- test database created through Testcontainers and Flyway

No migration should depend on production-only paths or manual database steps.

---

## Testing Strategy

### Migration Smoke Test

Create or update an integration test that verifies the application context starts and Flyway applies the schema successfully in the `test` profile.

Possible test targets:

- existing application context test
- dedicated migration integration test

### Schema Verification Test

Add a test class such as:

**`src/test/java/md/hashcode/vector9/integration/SchemaMigrationIntegrationTest.java`**

Verify:

- all expected tables exist
- the seed subcategory exists
- foreign keys are valid
- expected indexes exist where practical to assert

### Database Behavior Test

Add tests that validate:

- `owners.id` is UUID-backed
- `ad_images` rejects duplicate `(ad_id, image_filename)` values
- `view_history` does not contain `views_today`
- `ads.enrichment_status` defaults to `pending`

---

## Validation and Success Criteria

### Run Migrations via Tests

```powershell
mvn test
```

Expected result:

- Flyway migrations apply successfully on the test database
- schema-related tests pass
- no conflicting migration checksum or ordering issues

### Run Full Build

```powershell
mvn clean install
```

Expected result:

- build succeeds
- all tests pass
- no Flyway validation failures

### Manual Database Verification

After starting the dev database:

```powershell
./scripts/db-start.ps1
./scripts/db-psql.ps1
```

Inside `psql`, verify:

```sql
\dt
SELECT * FROM subcategories;
```

Expected result:

- all crawler tables exist
- one initial subcategory exists and is enabled

---

## Checklist

- [ ] Flyway migration created for the core crawler schema
- [ ] `subcategories` table created
- [ ] `owners` table created with UUID primary key and unique login
- [ ] `ads` table created with enrichment status tracking
- [ ] `ad_images` table created with filename deduplication support
- [ ] `price_history` table created
- [ ] `crawl_checkpoints` table created
- [ ] `car_features` table created
- [ ] `ad_attributes` table created
- [ ] `view_history` table created without `views_today`
- [ ] indexes added for crawler and history access patterns
- [ ] one initial subcategory seeded
- [ ] migrations run successfully in automated tests
- [ ] schema verified manually against the dev database

---

## Common Issues and Solutions

### Issue: Flyway reports checksum mismatch

**Solution**:

- do not edit already-applied migrations casually
- if a migration is still local-only, reset the database and rerun
- if it has already been shared, create a new forward migration instead of rewriting history

### Issue: Foreign key creation fails

**Solution**:

- verify table creation order
- create parent tables before dependent tables
- check column types match exactly

### Issue: Testcontainers migration test fails

**Solution**:

- ensure Docker is running
- ensure Flyway is enabled in `application-test.yml`
- inspect startup logs for the exact failing SQL statement

### Issue: Duplicate images are still possible

**Solution**:

- verify the unique index on `(ad_id, image_filename)` exists
- keep repository insert logic aligned with the append-only image policy in later tasks

---

## Deliverables

1. Flyway migration(s) for the production crawler schema
2. One seeded initial subcategory for controlled rollout
3. Schema verification tests
4. Manual verification instructions for the development database
5. A schema ready for Task 04 jOOQ code generation

---

## Next Steps

After completing this task:

1. Proceed to **Task 04: jOOQ Code Generation and Database Access Setup**
2. Generate jOOQ classes from the migrated schema
3. Begin implementing repositories against the generated schema

---

**Task Status**: Ready for Implementation
**Estimated Completion**: 3-5 hours
**Blocker For**: Task 04 (jOOQ Code Generation)
