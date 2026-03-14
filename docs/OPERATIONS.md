# Vector9 Operations

## Runtime Inputs

Production runtime values must come from environment variables:

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `VECTOR9_LOG_FILE` optional, defaults to `/var/log/vector9/application.log`
- `JAVA_OPTS` optional JVM flags

## Build and Run

Build the image:

```bash
docker build -t vector9:latest .
```

Run the application:

```bash
docker run --rm \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=<host> \
  -e DB_PORT=5432 \
  -e DB_NAME=<database> \
  -e DB_USER=<user> \
  -e DB_PASSWORD=<password> \
  vector9:latest
```

## Health Checks

Check overall health:

```bash
curl http://localhost:8080/actuator/health
```

Important components:

- `database`
- `incrementalCrawlerFreshness`
- `deletionDetectionFreshness`
- `viewTrackingFreshness`

If a job is stale or failing, the corresponding component will include the last success or failure details.

## Metrics

Metrics are exposed through:

```bash
curl http://localhost:8080/actuator/metrics
```

Key metric names:

- `vector9.graphql.requests`
- `vector9.graphql.request.duration`
- `vector9.jobs.executions`
- `vector9.jobs.execution.duration`
- `vector9.ads.processed`
- `vector9.ads.deleted`
- `vector9.views.updated_ads`
- `vector9.views.history_rows`

## Logs

Normal production logs are plain text and should stay readable in both containers and file sinks.

Look for:

- GraphQL request failures
- crawler subcategory failure messages
- stale health component details

## Recovery

1. Check `/actuator/health` for stale or failing components.
2. Verify database connectivity and current DB size from the `database` health details.
3. Review application logs around the last reported failure time.
4. Rerun the affected job manually or restart the service if the failure is transient.
5. Confirm the corresponding freshness component returns to a healthy state after the next successful execution.
