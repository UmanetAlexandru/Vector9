# Vector9

Vector9 is a Spring Boot service for scraping and enriching marketplace data from `999.md`.

## Requirements

- Java 21
- Maven 3.9+
- Docker running locally for integration tests

## Common Commands

```bash
mvn clean install
mvn test
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Profiles

- `dev` for local development
- `test` for automated tests with Testcontainers
- `prod` for production-oriented overrides
