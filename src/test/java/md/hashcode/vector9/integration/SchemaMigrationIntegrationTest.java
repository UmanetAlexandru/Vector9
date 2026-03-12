package md.hashcode.vector9.integration;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchemaMigrationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("vector9_test")
            .withUsername("vector9")
            .withPassword("vector9");

    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    void setUpDatabase() {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void shouldCreateExpectedTables() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                ORDER BY table_name
                """,
                String.class
        );

        assertThat(tables).contains(
                "subcategories",
                "owners",
                "ads",
                "ad_images",
                "price_history",
                "crawl_checkpoints",
                "car_features",
                "ad_attributes",
                "view_history"
        );
    }

    @Test
    void shouldSeedInitialSubcategory() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM subcategories
                WHERE id = 7661
                  AND enabled = true
                """,
                Integer.class
        );

        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldUseUuidAsOwnerPrimaryKey() {
        String typeName = jdbcTemplate.queryForObject(
                """
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'owners'
                  AND column_name = 'id'
                """,
                String.class
        );

        assertThat(typeName).isEqualTo("uuid");
    }

    @Test
    void shouldDefaultAdsEnrichmentStatusToPending() {
        String defaultValue = jdbcTemplate.queryForObject(
                """
                SELECT column_default
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'ads'
                  AND column_name = 'enrichment_status'
                """,
                String.class
        );

        assertThat(defaultValue).contains("pending");
    }

    @Test
    void shouldNotCreateViewsTodayInViewHistory() {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'view_history'
                      AND column_name = 'views_today'
                )
                """,
                Boolean.class
        );

        assertThat(exists).isFalse();
    }

    @Test
    void shouldCreateImageFilenameDeduplicationIndex() {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM pg_indexes
                    WHERE schemaname = 'public'
                      AND tablename = 'ad_images'
                      AND indexname = 'idx_ad_images_ad_id_filename'
                )
                """,
                Boolean.class
        );

        assertThat(exists).isTrue();
    }

    @Test
    void shouldRejectDuplicateImageFilenamesForSameAd() {
        jdbcTemplate.update(
                "INSERT INTO owners (id, login) VALUES (uuid_generate_v4(), ?)",
                "owner-for-image-test"
        );

        jdbcTemplate.update(
                """
                INSERT INTO ads (id, title, subcategory_id, owner_id)
                SELECT ?, ?, ?, id
                FROM owners
                WHERE login = ?
                """,
                1001L,
                "Schema test ad",
                7661L,
                "owner-for-image-test"
        );

        jdbcTemplate.update(
                """
                INSERT INTO ad_images (ad_id, image_filename, position, is_primary)
                VALUES (?, ?, ?, ?)
                """,
                1001L,
                "image-a.jpg",
                1,
                true
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO ad_images (ad_id, image_filename, position, is_primary)
                VALUES (?, ?, ?, ?)
                """,
                1001L,
                "image-a.jpg",
                2,
                false
        )).isInstanceOf(Exception.class);
    }
}