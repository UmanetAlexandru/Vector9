package md.hashcode.vector9.integration;

import md.hashcode.vector9.jooq.Tables;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqGeneratedSchemaIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    private DSLContext dslContext;

    @BeforeAll
    void setUp() {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        dslContext = DSL.using(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
    }

    @Test
    void shouldQuerySeededSubcategoryUsingGeneratedTables() {
        String subcategoryName = dslContext
                .select(Tables.SUBCATEGORIES.NAME)
                .from(Tables.SUBCATEGORIES)
                .where(Tables.SUBCATEGORIES.ID.eq(7661L))
                .fetchOne(Tables.SUBCATEGORIES.NAME);

        assertThat(subcategoryName).isEqualTo("Mini PC");
    }
}
