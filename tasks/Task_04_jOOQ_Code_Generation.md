# Task 04: jOOQ Code Generation and Database Access Setup

## Overview

Configure jOOQ code generation for the Vector9 production schema created in Task 03. This task should make the generated schema classes available to the application and tests, while keeping generation explicit and reproducible against the local PostgreSQL database used for development.

**Estimated Time**: 2-4 hours
**Priority**: Critical
**Dependencies**: Task 03 (Database Schema and Flyway Migrations)

---

## Objectives

1. Add Maven configuration for reproducible jOOQ code generation
2. Run Flyway migrations before generation so generated classes always reflect the current schema
3. Generate jOOQ classes for the production crawler tables
4. Make generated sources available to the main build and tests
5. Verify that Spring Boot can use `DSLContext` together with the generated schema

---

## Implementation Notes

- Keep using PostgreSQL as the source of truth for code generation
- Generate code from the migrated development schema, not from handwritten SQL parsing
- Exclude Flyway bookkeeping tables such as `flyway_schema_history`
- Keep generation explicit through a Maven profile instead of forcing it on every `mvn test`
- Commit generated sources so later repository tasks can build without requiring local regeneration first
- Do not implement repositories in this task; that belongs to the next persistence-layer task

---

## Implementation Steps

### Step 1: Add Maven jOOQ Code Generation Configuration

Update `pom.xml` to add:

- `jooq-codegen-maven`
- `build-helper-maven-plugin` so generated sources are compiled
- `flyway-maven-plugin` in the same profile so schema migration can run before generation

Recommended profile name:

```text
jooq-codegen
```

The profile should:

- connect to the local PostgreSQL database used for development
- run Flyway `migrate`
- run jOOQ `generate`

### Step 2: Define Generation Output and Package

Recommended output directory:

```text
src/main/generated/jooq
```

Recommended package root:

```text
md.hashcode.vector9.jooq
```

Generate classes for the production tables created in Task 03:

- `subcategories`
- `owners`
- `ads`
- `ad_images`
- `price_history`
- `crawl_checkpoints`
- `car_features`
- `ad_attributes`
- `view_history`

### Step 3: Keep Generation Scope Focused

Generation rules:

- input schema: `public`
- exclude `flyway_schema_history`
- generate table references, records, and POJOs only if clearly useful now
- keep nullable and default metadata intact

Avoid adding:

- custom generator extensions
- forced type mappings unless the schema proves they are needed
- repository implementations

### Step 4: Add Build and Runtime Verification

Add an integration test that proves:

- the generated table classes are on the test classpath
- Spring Boot provides a working `DSLContext`
- a simple query using generated jOOQ tables works against the migrated test database

Recommended test location:

```text
src/test/java/md/hashcode/vector9/integration/JooqIntegrationTest.java
```

### Step 5: Document Regeneration Workflow

Document the one-command regeneration workflow for future schema tasks:

```powershell
mvn -Pjooq-codegen generate-sources
```

Developers should know that:

- Docker/PostgreSQL must be running
- the dev database must be reachable with the configured credentials
- the profile itself will apply migrations before generation
- host and port can be overridden with Maven properties if the default local port is already in use

---

## Testing Strategy

### Code Generation Validation

Run:

```powershell
mvn -Pjooq-codegen generate-sources
```

Expected result:

- Flyway migrations run successfully
- jOOQ generated classes are written under `src/main/generated/jooq`
- no schema introspection errors occur

### Build Validation

Run:

```powershell
mvn test
```

Expected result:

- generated sources compile as part of the build
- integration tests can use `DSLContext`
- no missing generated-type errors occur

---

## Checklist

- [ ] Task 04 task file created
- [ ] jOOQ Maven profile added
- [ ] Flyway migration step wired before generation
- [ ] generated source directory added to the build
- [ ] generated schema classes created for production tables
- [ ] Flyway history table excluded from generation
- [ ] integration test added for generated jOOQ access
- [ ] regeneration command documented and verified

---

## Deliverables

1. Maven jOOQ code generation configuration
2. Generated jOOQ schema classes committed to the repository
3. Integration test covering `DSLContext` and generated tables
4. Task documentation for regeneration workflow

---

## Next Steps

After completing this task:

1. Proceed to repository implementations using generated jOOQ tables
2. Add ad, owner, image, and checkpoint persistence services
3. Build GraphQL ingestion on top of the repository layer

---

**Task Status**: Ready for Implementation
**Estimated Completion**: 2-4 hours
**Blocker For**: Repository Layer Task
