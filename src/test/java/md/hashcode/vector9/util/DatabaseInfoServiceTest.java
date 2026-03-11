package md.hashcode.vector9.util;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseInfoServiceTest {

    @Autowired
    private DatabaseInfoService databaseInfoService;

    @Test
    void shouldGetDatabaseInfo() {
        DatabaseInfoService.DatabaseInfo info = databaseInfoService.getDatabaseInfo();

        assertThat(info.version()).isNotBlank();
        assertThat(info.currentDatabase()).isNotBlank();
        assertThat(info.databaseSize()).isNotBlank();
    }

    @Test
    void shouldGetTableSizes() {
        List<DatabaseInfoService.TableInfo> tables = databaseInfoService.getTableSizes();

        assertThat(tables).isNotNull();
    }

    @Test
    void shouldGetActiveConnectionCount() {
        int count = databaseInfoService.getActiveConnectionCount();

        assertThat(count).isGreaterThan(0);
    }
}