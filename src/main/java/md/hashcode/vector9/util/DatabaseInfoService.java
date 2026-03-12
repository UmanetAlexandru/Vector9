package md.hashcode.vector9.util;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseInfoService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInfoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DatabaseInfo getDatabaseInfo() {
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
        String currentDatabase = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
        String databaseSize = jdbcTemplate.queryForObject(
                "SELECT pg_size_pretty(pg_database_size(current_database()))",
                String.class
        );
        return new DatabaseInfo(version, currentDatabase, databaseSize);
    }

    public List<TableInfo> getTableSizes() {
        String sql = """
                SELECT schemaname,
                       tablename,
                       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
                       pg_total_relation_size(schemaname||'.'||tablename) AS size_bytes
                FROM pg_tables
                WHERE schemaname = 'public'
                ORDER BY size_bytes DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new TableInfo(
                rs.getString("schemaname"),
                rs.getString("tablename"),
                rs.getString("size"),
                rs.getLong("size_bytes")
        ));
    }

    public int getActiveConnectionCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()",
                Integer.class
        );
        return count == null ? 0 : count;
    }

    public record DatabaseInfo(String version, String currentDatabase, String databaseSize) {
    }

    public record TableInfo(String schema, String tableName, String size, Long sizeBytes) {
    }
}