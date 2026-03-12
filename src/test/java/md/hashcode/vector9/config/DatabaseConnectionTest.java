package md.hashcode.vector9.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseConnectionTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldConnectToDatabase() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void shouldHavePostgreSqlVersion() {
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
        assertThat(version).containsIgnoringCase("PostgreSQL");
    }

    @Test
    void shouldHaveUuidExtension() {
        Boolean hasExtension = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname = 'uuid-ossp')",
                Boolean.class
        );
        assertThat(hasExtension).isTrue();
    }

    @Test
    void shouldGenerateUuid() {
        String uuid = jdbcTemplate.queryForObject("SELECT uuid_generate_v4()::text", String.class);
        assertThat(uuid).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}