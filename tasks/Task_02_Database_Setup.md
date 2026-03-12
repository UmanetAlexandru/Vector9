# Task 02: Database Setup with Docker

## Overview
Set up PostgreSQL with Docker Compose for local development on Windows, configure the application connection, and add database health and utility services.

**Estimated Time**: 1-2 hours
**Priority**: Critical
**Dependencies**: Task 01 (Project Setup)

---

## Objectives

1. Create Docker Compose configuration for PostgreSQL
2. Configure safe database initialization scripts
3. Set up database connection in the application
4. Verify database connectivity
5. Create database management utilities for Windows development

---

## Implementation Notes

- Development environment is Windows
- Production environment is Linux
- Prefer PowerShell scripts for local developer workflows in this task
- Keep Docker Compose setup portable so it can still be used from Linux if needed
- Use a single PostgreSQL version consistently across the task
- Use `postgres:18-alpine`
- Do not put server-level tuning settings into database init SQL
- Keep `.env` ignored and commit a `.env.example` template instead
- Implement code in a Spring Boot 4 compatible style; do not copy older Boot 2/3 actuator imports directly

---

## Implementation Steps

### Step 1: Create Docker Compose File

**`docker-compose.yml`** (project root):

```yaml
services:
  postgres:
    image: postgres:18-alpine
    container_name: vector9-postgres
    environment:
      POSTGRES_DB: vector9_dev
      POSTGRES_USER: vector9
      POSTGRES_PASSWORD: vector9_dev_password
      POSTGRES_INITDB_ARGS: "-E UTF8"
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./docker/postgres/init:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U vector9 -d vector9_dev"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s
    networks:
      - vector9-network
    restart: unless-stopped

  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: vector9-pgadmin
    environment:
      PGADMIN_DEFAULT_EMAIL: ${PGADMIN_EMAIL:-admin@vector9.local}
      PGADMIN_DEFAULT_PASSWORD: ${PGADMIN_PASSWORD:-admin}
      PGADMIN_CONFIG_SERVER_MODE: "False"
    ports:
      - "5050:80"
    volumes:
      - pgadmin_data:/var/lib/pgadmin
    networks:
      - vector9-network
    depends_on:
      - postgres
    restart: unless-stopped
    profiles:
      - tools

volumes:
  postgres_data:
    driver: local
  pgadmin_data:
    driver: local

networks:
  vector9-network:
    driver: bridge
```

### Step 2: Create Database Initialization Scripts

Create directory:

```bash
mkdir -p docker/postgres/init
```

Only include safe, valid initialization logic here: extensions, roles, grants, and optional schema bootstrap helpers.

**`docker/postgres/init/01_create_extensions.sql`**:

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "btree_gin";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

DO $$
BEGIN
    RAISE NOTICE 'PostgreSQL extensions created successfully';
END $$;
```

**`docker/postgres/init/02_create_roles.sql`**:

```sql
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'vector9_readonly') THEN
        CREATE ROLE vector9_readonly WITH LOGIN PASSWORD 'readonly_password';
    END IF;
END $$;

GRANT CONNECT ON DATABASE vector9_dev TO vector9_readonly;

DO $$
BEGIN
    RAISE NOTICE 'Database roles created successfully';
END $$;
```

Do not add SQL files that attempt to change cluster/server settings such as:

- `max_connections`
- `shared_buffers`
- `wal_buffers`
- `effective_cache_size`

Those belong in PostgreSQL server configuration, not per-database init scripts for this task.

### Step 3: Create Environment Template

**`.env.example`** (project root):

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=vector9_dev
DB_USER=vector9
DB_PASSWORD=vector9_dev_password

# PgAdmin Configuration
PGADMIN_EMAIL=admin@vector9.local
PGADMIN_PASSWORD=admin

# Application Configuration
SPRING_PROFILES_ACTIVE=dev
```

Notes:

- Commit `.env.example`
- Add `.env` to `.gitignore`
- Developers copy `.env.example` to `.env` locally and adjust values if needed

Add to **`.gitignore`**:

```text
.env
.env.local
postgres_data/
pgadmin_data/
```

### Step 4: Create Database Management Scripts

Because local development is on Windows, provide PowerShell scripts.

Create:

- `scripts/db-start.ps1`
- `scripts/db-stop.ps1`
- `scripts/db-reset.ps1`
- `scripts/db-logs.ps1`
- `scripts/db-psql.ps1`

These scripts should:

- use `docker compose` rather than legacy `docker-compose`
- optionally load values from `.env`
- work from PowerShell on Windows

Example responsibilities:

**`scripts/db-start.ps1`**

- start `postgres`
- wait for health/readiness
- print connection info

**`scripts/db-stop.ps1`**

- stop the database services

**`scripts/db-reset.ps1`**

- warn before deleting volumes
- recreate the database cleanly

**`scripts/db-logs.ps1`**

- stream PostgreSQL logs

**`scripts/db-psql.ps1`**

- open a `psql` session inside the running postgres container

Optional:

- Add Linux shell equivalents later if needed for production-adjacent operational workflows
- Production deployment itself is not part of this task

### Step 5: Create Database Health Check Service

**`src/main/java/md/hashcode/vector9/config/DatabaseHealthIndicator.java`**:

Implement a custom database health indicator using `JdbcTemplate`.

Requirements:

- verify connection with a simple query
- include PostgreSQL version
- include database size
- include active connection count
- use Spring Boot 4 health APIs
- avoid Lombok if it does not improve clarity

### Step 6: Create Database Utility Service

**`src/main/java/md/hashcode/vector9/util/DatabaseInfoService.java`**

Implement a utility/service component that exposes:

- database version
- current database name
- database size
- public table sizes
- active connection count

Simple Java records or plain classes are preferred over Lombok-generated DTOs unless Lombok is already clearly justified.

### Step 7: Update Application Configuration

Add to **`application-dev.yml`**:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/vector9_dev
    username: vector9
    password: vector9_dev_password
    hikari:
      maximum-pool-size: 5
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000

logging:
  level:
    org.springframework.jdbc: DEBUG
    com.zaxxer.hikari: DEBUG
```

Keep production configuration separate from local Docker assumptions.

---

## Testing Strategy

### Database Connection Test

**`src/test/java/md/hashcode/vector9/config/DatabaseConnectionTest.java`**

Test:

- database connectivity
- PostgreSQL version availability
- required extension availability
- UUID generation

Use Spring Boot 4 compatible imports and assertions.

### Health Indicator Test

**`src/test/java/md/hashcode/vector9/config/DatabaseHealthIndicatorTest.java`**

Test:

- health status is `UP`
- details contain version, database size, and active connection count

### Database Info Service Test

**`src/test/java/md/hashcode/vector9/util/DatabaseInfoServiceTest.java`**

Test:

- basic database info is returned
- table sizes query returns a non-null list
- active connection count is positive

### Integration Test

**`src/test/java/md/hashcode/vector9/integration/DatabaseIntegrationTest.java`**

Test:

- actuator health reports database component
- database details are exposed in the health payload

Use the same test style already established in Task 01.

---

## Validation and Success Criteria

### Start Database

```powershell
./scripts/db-start.ps1
```

Expected result:

- PostgreSQL container starts
- readiness check passes
- connection details are printed

### Verify Database Running

```powershell
docker compose ps
```

Expected result:

- `vector9-postgres` is `Up (healthy)`

### Connect to Database

```powershell
./scripts/db-psql.ps1
```

Expected result:

- `psql` opens against `vector9_dev`

Inside `psql`, verify:

```sql
\l
\dx
```

Extensions should include:

- `uuid-ossp`
- `btree_gin`
- `pg_trgm`

### Test Application Connection

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected result:

- application starts successfully
- database connection is established

### Check Health Endpoint

```powershell
curl http://localhost:8080/actuator/health
```

Expected result:

- overall status is `UP`
- custom database health details are present

### Test Suite Validation

```powershell
mvn test
```

Expected result:

- database-related tests pass
- existing Task 01 tests continue to pass

---

## Checklist

- [ ] Docker Compose file created
- [ ] Database initialization scripts created
- [ ] `.env.example` created
- [ ] `.env` ignored in `.gitignore`
- [ ] PowerShell database management scripts created
- [ ] `DatabaseHealthIndicator` created
- [ ] `DatabaseInfoService` created
- [ ] Application connects to database successfully
- [ ] Health endpoint shows database status
- [ ] All relevant tests pass
- [ ] Database can be started/stopped/reset from PowerShell
- [ ] PgAdmin accessible with `--profile tools` (optional)

---

## Common Issues and Solutions

### Issue: Port 5432 already in use

**Solution**:

- stop the conflicting local PostgreSQL instance
- or change the published port in `docker-compose.yml`

### Issue: Docker is not running

**Solution**:

- start Docker Desktop
- verify with `docker ps`

### Issue: Testcontainers cannot start PostgreSQL

**Solution**:

- ensure Docker Desktop is running
- re-run `mvn test`
- inspect the failing test logs

### Issue: PowerShell script execution is blocked

**Solution**:

Run with an appropriate execution policy for local scripts, for example:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\db-start.ps1
```

### Issue: Database needs a clean reset

**Solution**:

```powershell
./scripts/db-reset.ps1
```

---

## PgAdmin Setup (Optional)

To use PgAdmin:

```powershell
docker compose --profile tools up -d
```

Access:

- URL: `http://localhost:5050`
- Email: `admin@vector9.local`
- Password: `admin`

Recommended server settings in PgAdmin:

- Name: `Vector9 Development`
- Host: `postgres`
- Port: `5432`
- Database: `vector9_dev`
- Username: `vector9`
- Password: `vector9_dev_password`

---

## Deliverables

1. Docker Compose configuration with PostgreSQL 18
2. Safe database initialization scripts for extensions and roles
3. PowerShell database management scripts for Windows development
4. `DatabaseHealthIndicator` with database details
5. `DatabaseInfoService` for database statistics
6. Passing database-related tests
7. Health endpoint showing database status
8. Documentation for local database management
9. `.env.example` template for local setup

---

## Next Steps

After completing this task:

1. Proceed to **Task 03: Database Schema & Flyway Migrations**
2. Create tables such as subcategories, ads, owners, and related entities
3. Configure jOOQ code generation after schema creation

---

**Task Status**: Ready for Implementation
**Estimated Completion**: 1-2 hours
**Blocker For**: Task 03 (Database Schema)
