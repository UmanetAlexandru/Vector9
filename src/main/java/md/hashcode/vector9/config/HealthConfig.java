package md.hashcode.vector9.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

@Component("vector9")
public class HealthConfig implements HealthIndicator {

    private final BuildProperties buildProperties;

    public HealthConfig(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("status", "Vector9 is running")
                .withDetail("name", buildProperties.getName())
                .withDetail("version", buildProperties.getVersion())
                .build();
    }
}