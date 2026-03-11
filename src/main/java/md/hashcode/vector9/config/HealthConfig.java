package md.hashcode.vector9.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
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