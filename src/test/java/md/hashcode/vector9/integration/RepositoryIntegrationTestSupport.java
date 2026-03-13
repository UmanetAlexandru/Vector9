package md.hashcode.vector9.integration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import md.hashcode.vector9.repository.AdImageRepository;
import md.hashcode.vector9.repository.AdRepository;
import md.hashcode.vector9.repository.CrawlCheckpointRepository;
import md.hashcode.vector9.repository.OwnerRepository;
import md.hashcode.vector9.repository.PriceHistoryRepository;
import md.hashcode.vector9.repository.SubcategoryRepository;
import md.hashcode.vector9.service.AdPersistenceService;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class RepositoryIntegrationTestSupport {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    protected Flyway flyway;
    protected DSLContext dslContext;
    protected SubcategoryRepository subcategoryRepository;
    protected OwnerRepository ownerRepository;
    protected AdRepository adRepository;
    protected AdImageRepository adImageRepository;
    protected PriceHistoryRepository priceHistoryRepository;
    protected CrawlCheckpointRepository crawlCheckpointRepository;
    protected AdPersistenceService adPersistenceService;

    @BeforeEach
    void resetDatabase() throws SQLException {
        DataSource dataSource = new SimpleDriverDataSource(
                POSTGRES.getJdbcDriverInstance(),
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );

        if (flyway == null) {
            flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .cleanDisabled(false)
                    .load();
        }

        flyway.clean();
        flyway.migrate();

        Connection connection = dataSource.getConnection();
        dslContext = DSL.using(connection, SQLDialect.POSTGRES);
        subcategoryRepository = new SubcategoryRepository(dslContext);
        ownerRepository = new OwnerRepository(dslContext);
        adRepository = new AdRepository(dslContext);
        adImageRepository = new AdImageRepository(dslContext);
        priceHistoryRepository = new PriceHistoryRepository(dslContext);
        crawlCheckpointRepository = new CrawlCheckpointRepository(dslContext);
        adPersistenceService = new AdPersistenceService(
                ownerRepository,
                adRepository,
                adImageRepository,
                priceHistoryRepository,
                new DataSourceTransactionManager(dataSource)
        );
    }
}