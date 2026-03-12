package md.hashcode.vector9.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseHealthIndicatorTest {

    @Autowired
    private DatabaseHealthIndicator healthIndicator;

    @Test
    void shouldReportHealthyDatabase() {
        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKeys("version", "databaseSize", "activeConnections");
    }

    @Test
    void shouldIncludePostgreSqlVersion() {
        Health health = healthIndicator.health();
        assertThat(health.getDetails().get("version").toString()).containsIgnoringCase("PostgreSQL");
    }
}