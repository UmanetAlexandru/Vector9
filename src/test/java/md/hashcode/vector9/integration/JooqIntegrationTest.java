package md.hashcode.vector9.integration;

import md.hashcode.vector9.jooq.Tables;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JooqIntegrationTest {

    @Autowired
    private DSLContext dslContext;

    @Test
    void shouldProvideSpringManagedDslContext() {
        assertThat(dslContext).isNotNull();
        assertThat(dslContext.dialect()).isEqualTo(SQLDialect.POSTGRES);
    }

    @Test
    void shouldExposeGeneratedAdsTableMetadata() {
        assertThat(Tables.ADS.getSchema()).isNotNull();
        assertThat(Tables.ADS.getSchema().getName()).isEqualTo("public");
        assertThat(Tables.ADS.getPrimaryKey()).isNotNull();
        assertThat(Tables.ADS.field(Tables.ADS.ENRICHMENT_STATUS)).isNotNull();
    }
}
