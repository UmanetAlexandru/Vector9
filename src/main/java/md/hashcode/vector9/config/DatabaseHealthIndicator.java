package md.hashcode.vector9.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("database")
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
            Long databaseSize = jdbcTemplate.queryForObject(
                    "SELECT pg_database_size(current_database())",
                    Long.class
            );
            Integer activeConnections = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()",
                    Integer.class
            );

            return Health.up()
                    .withDetail("version", version)
                    .withDetail("databaseSize", formatBytes(databaseSize))
                    .withDetail("activeConnections", activeConnections)
                    .build();
        }
        catch (Exception exception) {
            return Health.down()
                    .withException(exception)
                    .build();
        }
    }

    private String formatBytes(Long bytes) {
        if (bytes == null) {
            return "Unknown";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}