# Task 01: Project Setup and Dependencies

## Overview
Initialize the Vector9 Spring Boot project with the required dependencies, build configuration, and basic project structure.

**Estimated Time**: 2-3 hours
**Priority**: Critical (Blocker for all other tasks)
**Dependencies**: None

---

## Objectives

1. Create or align the project with the latest stable Spring Boot version compatible with Java 21
2. Configure Maven with the required dependencies for the current phase
3. Set up the basic project structure
4. Configure application properties for local development, test, and production
5. Verify build and basic application startup

---

## Implementation Notes

- Use `groupId=md.hashcode` and `artifactId=vector9`
- Use the latest stable Spring Boot release that works cleanly with the selected dependencies
- Skip Playwright in this task to keep setup lean; add it in a later crawling/parsing task
- Docker is an accepted local prerequisite for running Testcontainers-based tests
- Do not enable jOOQ code generation yet; that belongs to a later task once the database schema exists

---

## Implementation Steps

### Step 1: Align Project Coordinates and Base Setup

Use the existing IntelliJ-generated Spring Boot project as the starting point and update it as needed.

Target Maven coordinates:

- `groupId`: `md.hashcode`
- `artifactId`: `vector9`
- `name`: `Vector9`

### Step 2: Update `pom.xml` with Required Dependencies

Use the latest stable Spring Boot parent compatible with Java 21.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version><!-- latest stable compatible version --></version>
        <relativePath/>
    </parent>

    <groupId>md.hashcode</groupId>
    <artifactId>vector9</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>Vector9</name>
    <description>Web scraping system for 999.md marketplace</description>

    <properties>
        <java.version>21</java.version>
        <jooq.version>3.20.11</jooq.version>
        <testcontainers.version>1.21.4</testcontainers.version>
        <okhttp.version>5.3.2</okhttp.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- jOOQ runtime support -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jooq</artifactId>
        </dependency>

        <!-- HTTP Client -->
        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>okhttp</artifactId>
            <version>${okhttp.version}</version>
        </dependency>

        <!-- JSON Processing -->
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <!-- Utilities -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>jdbc</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>mockwebserver</artifactId>
            <version>${okhttp.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Notes:

- Do not configure `jooq-codegen-maven` in this task
- Do not add Playwright yet
- Do not expose Prometheus unless the Prometheus registry dependency is also added

### Step 3: Create Project Package Structure

```text
src/
|-- main/
|   |-- java/md/hashcode/vector9/
|   |   |-- Vector9Application.java
|   |   |-- config/              # Configuration classes
|   |   |-- client/              # HTTP clients
|   |   |-- crawler/             # Crawling services
|   |   |-- enrichment/          # Detail enrichment services
|   |   |-- model/               # Domain models and DTOs
|   |   |   |-- dto/             # Data transfer objects
|   |   |   |-- persistence/     # Persistence-facing models if needed
|   |   |   `-- graphql/         # GraphQL request/response models
|   |   |-- repository/          # Data access layer
|   |   |-- service/             # Business logic
|   |   |-- scheduler/           # Scheduled jobs
|   |   `-- util/                # Utilities
|   `-- resources/
|       |-- application.yml
|       |-- application-dev.yml
|       |-- application-prod.yml
|       `-- db/migration/        # Flyway migrations
`-- test/
    |-- java/md/hashcode/vector9/
    |   |-- integration/         # Integration tests
    |   |-- client/              # Client tests
    |   |-- service/             # Service tests
    |   `-- repository/          # Repository tests
    `-- resources/
        `-- application-test.yml
```

Create the directory structure:

```bash
mkdir -p src/main/java/md/hashcode/vector9/{config,client,crawler,enrichment,model/{dto,persistence,graphql},repository,service,scheduler,util}
mkdir -p src/main/resources/db/migration
mkdir -p src/test/java/md/hashcode/vector9/{integration,client,service,repository}
mkdir -p src/test/resources
```

### Step 4: Configure `application.yml`

**`src/main/resources/application.yml`**

```yaml
spring:
  application:
    name: vector9

  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:vector9}
    username: ${DB_USER:vector9}
    password: ${DB_PASSWORD:vector9}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true

  jooq:
    sql-dialect: POSTGRES

  jackson:
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
  health:
    db:
      enabled: true

logging:
  level:
    root: INFO
    md.hashcode.vector9: DEBUG
    org.jooq: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n"
  file:
    name: ${VECTOR9_LOG_FILE:logs/vector9.log}
    max-size: 100MB
    max-history: 30

vector9:
  graphql:
    base-url: https://999.md/graphql
    timeout-ms: 30000

  crawler:
    enabled: false
    schedule-cron: "0 */15 * * * *"
    request-delay-ms: 1000
    jitter-ms: 200
    page-size: 50

  enrichment:
    enabled: false
    schedule-cron: "0 0 1 * * *"
    batch-size: 20
    delay-ms: 3000
    refresh-days: 30
```

**`src/main/resources/application-dev.yml`**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/vector9_dev

  flyway:
    clean-disabled: false

logging:
  level:
    md.hashcode.vector9: DEBUG
    org.jooq.tools.LoggerListener: DEBUG

vector9:
  graphql:
    timeout-ms: 60000

  crawler:
    request-delay-ms: 2000
```

**`src/test/resources/application-test.yml`**

```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:18-alpine:///vector9_test
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver

  flyway:
    enabled: true
    clean-disabled: false

logging:
  level:
    root: WARN
    md.hashcode.vector9: DEBUG
```

**`src/main/resources/application-prod.yml`**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20

  flyway:
    clean-disabled: true

logging:
  level:
    root: INFO
    md.hashcode.vector9: INFO
    org.jooq: WARN
  file:
    name: ${VECTOR9_LOG_FILE:/var/log/vector9/application.log}

vector9:
  crawler:
    enabled: true

  enrichment:
    enabled: true
```

Notes:

- Do not set `spring.profiles.active` inside `application.yml`
- Select the active profile via environment variable, IDE configuration, or command line
- Keep production file logging externally overridable

### Step 5: Create Main Application Class

**`src/main/java/md/hashcode/vector9/Vector9Application.java`**

```java
package md.hashcode.vector9;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Vector9Application {

    public static void main(String[] args) {
        SpringApplication.run(Vector9Application.class, args);
    }
}
```

### Step 6: Create Initial Configuration Classes

**`src/main/java/md/hashcode/vector9/config/JacksonConfig.java`**

```java
package md.hashcode.vector9.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

**`src/main/java/md/hashcode/vector9/config/OkHttpConfig.java`**

```java
package md.hashcode.vector9.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class OkHttpConfig {

    @Value("${vector9.graphql.timeout-ms}")
    private long timeoutMs;

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .readTimeout(Duration.ofMillis(timeoutMs))
                .writeTimeout(Duration.ofMillis(timeoutMs))
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .build();
    }
}
```

### Step 7: Create Basic Health Check

**`src/main/java/md/hashcode/vector9/config/HealthConfig.java`**

```java
package md.hashcode.vector9.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("vector9")
public class HealthConfig implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up()
                .withDetail("status", "Vector9 is running")
                .withDetail("version", "1.0.0-SNAPSHOT")
                .build();
    }
}
```

### Step 8: Create Utility Classes

**`src/main/java/md/hashcode/vector9/util/CollectionUtils.java`**

```java
package md.hashcode.vector9.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection utilities to replace Guava dependencies.
 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    /**
     * Partition a list into smaller sublists of a specified size.
     *
     * @param list the list to partition
     * @param batchSize the size of each partition
     * @return list of copied sublists
     */
    public static <T> List<List<T>> partition(List<T> list, int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(new ArrayList<>(
                    list.subList(i, Math.min(i + batchSize, list.size()))
            ));
        }
        return partitions;
    }
}
```

---

## Testing Strategy

### Unit Tests

Create a basic application context test:

**`src/test/java/md/hashcode/vector9/Vector9ApplicationTests.java`**

```java
package md.hashcode.vector9;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class Vector9ApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

**`src/test/java/md/hashcode/vector9/config/JacksonConfigTest.java`**

```java
package md.hashcode.vector9.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSerializeInstantAsIsoString() throws Exception {
        Instant now = Instant.parse("2026-03-10T12:00:00Z");
        String json = objectMapper.writeValueAsString(now);

        assertThat(json).contains("2026-03-10T12:00:00Z");
        assertThat(json).doesNotContain("1709827200");
    }

    @Test
    void shouldDeserializeIsoStringToInstant() throws Exception {
        String json = "\"2026-03-10T12:00:00Z\"";
        Instant instant = objectMapper.readValue(json, Instant.class);

        assertThat(instant).isEqualTo(Instant.parse("2026-03-10T12:00:00Z"));
    }
}
```

**`src/test/java/md/hashcode/vector9/config/OkHttpConfigTest.java`**

```java
package md.hashcode.vector9.config;

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OkHttpConfigTest {

    @Autowired
    private OkHttpClient okHttpClient;

    @Test
    void shouldConfigureHttpClient() {
        assertThat(okHttpClient).isNotNull();
        assertThat(okHttpClient.connectTimeoutMillis()).isGreaterThan(0);
        assertThat(okHttpClient.readTimeoutMillis()).isGreaterThan(0);
    }

    @Test
    void shouldEnableRetryOnConnectionFailure() {
        assertThat(okHttpClient.retryOnConnectionFailure()).isTrue();
    }
}
```

**`src/test/java/md/hashcode/vector9/util/CollectionUtilsTest.java`**

```java
package md.hashcode.vector9.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CollectionUtilsTest {

    @Test
    void shouldPartitionListEvenly() {
        List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        List<List<Integer>> partitions = CollectionUtils.partition(list, 3);

        assertThat(partitions).hasSize(3);
        assertThat(partitions.get(0)).containsExactly(1, 2, 3);
        assertThat(partitions.get(1)).containsExactly(4, 5, 6);
        assertThat(partitions.get(2)).containsExactly(7, 8, 9);
    }

    @Test
    void shouldPartitionListUnevenly() {
        List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7, 8);
        List<List<Integer>> partitions = CollectionUtils.partition(list, 3);

        assertThat(partitions).hasSize(3);
        assertThat(partitions.get(0)).containsExactly(1, 2, 3);
        assertThat(partitions.get(1)).containsExactly(4, 5, 6);
        assertThat(partitions.get(2)).containsExactly(7, 8);
    }

    @Test
    void shouldHandleEmptyList() {
        List<Integer> list = List.of();
        List<List<Integer>> partitions = CollectionUtils.partition(list, 3);

        assertThat(partitions).isEmpty();
    }

    @Test
    void shouldHandleSingleElement() {
        List<Integer> list = List.of(1);
        List<List<Integer>> partitions = CollectionUtils.partition(list, 3);

        assertThat(partitions).hasSize(1);
        assertThat(partitions.get(0)).containsExactly(1);
    }

    @Test
    void shouldThrowExceptionForInvalidBatchSize() {
        List<Integer> list = List.of(1, 2, 3);

        assertThatThrownBy(() -> CollectionUtils.partition(list, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Batch size must be positive");

        assertThatThrownBy(() -> CollectionUtils.partition(list, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Batch size must be positive");
    }
}
```

### Integration Test

**`src/test/java/md/hashcode/vector9/integration/ApplicationIntegrationTest.java`**

```java
package md.hashcode.vector9.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApplicationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldExposeHealthEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/health",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void shouldExposeInfoEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/info",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

---

## Validation and Success Criteria

### Build Validation

```bash
mvn clean install
```

Expected result:

- Build completes successfully
- No jOOQ code generation failure
- Tests pass

### Application Startup

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected result:

- Application starts successfully
- No unresolved datasource or Flyway configuration errors beyond expected local environment prerequisites

### Test Execution

```bash
mvn test
```

Expected result:

- All defined tests pass
- Testcontainers starts PostgreSQL successfully using local Docker

### Health Check Validation

```bash
curl http://localhost:8080/actuator/health
```

Expected result:

```json
{
  "status": "UP"
}
```

At minimum, the endpoint must respond successfully and report an overall `UP` status once the application is running with a valid datasource.

---

## Checklist

- [ ] Maven coordinates updated to `md.hashcode:vector9`
- [ ] Java 21 configured
- [ ] Latest stable Spring Boot version selected
- [ ] Required dependencies added to `pom.xml`
- [ ] jOOQ runtime support added without enabling code generation yet
- [ ] Package structure created
- [ ] `application.yml`, `application-dev.yml`, `application-test.yml`, and `application-prod.yml` created
- [ ] Main application class created or updated
- [ ] Configuration classes created (`JacksonConfig`, `OkHttpConfig`, `HealthConfig`)
- [ ] Utility class created (`CollectionUtils`)
- [ ] `mvn clean install` succeeds
- [ ] `mvn test` passes
- [ ] Application starts without errors in the dev profile
- [ ] Health endpoint responds with `UP`
- [ ] Local logs directory is created when file logging is enabled

---

## Common Issues and Solutions

### Issue: "Cannot resolve symbol 'lombok'"

**Solution**:

1. Enable annotation processing in the IDE
2. Install the Lombok plugin for IntelliJ or Eclipse
3. Run `mvn clean install` to download dependencies

### Issue: "Database connection failed"

**Solution**:

- For development: start local PostgreSQL or prepare Docker-based infrastructure in the next task
- For tests: ensure Docker is running locally so Testcontainers can start PostgreSQL

### Issue: Tests fail with Testcontainers startup errors

**Solution**:

1. Verify Docker is running
2. Verify the Testcontainers dependencies are present
3. Re-run with `mvn clean test`

### Issue: Port 8080 already in use

**Solution**:

Set a different port in configuration or at runtime:

```yaml
server:
  port: 8081
```

---

## Deliverables

1. Working Maven project with the required dependencies
2. Proper package structure
3. Configuration files for all environments
4. Passing bootstrap and configuration tests
5. Application starts successfully
6. Health endpoint is accessible
7. `CollectionUtils` for list partitioning
8. `README.md` with setup instructions

---

## Next Steps

After completing this task:

1. Proceed to **Task 02: Database Setup** (PostgreSQL + Docker)
2. Then **Task 03: Database Schema** (Flyway migrations)
3. Then **Task 04: jOOQ Configuration** (code generation)

---

**Task Status**: Ready for Implementation
**Estimated Completion**: 2-3 hours
**Blocker For**: All subsequent tasks
