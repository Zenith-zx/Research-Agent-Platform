package com.selfinvestor.earnings;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flyway;MODE=MySQL;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=true"
})
class FlywayMigrationTest {
    @Resource
    private JdbcTemplate jdbc;

    /**
     * 验证 Flyway 能创建核心表和唯一约束。
     */
    @Test
    void migrationCreatesCoreTablesAndAccessionUniqueConstraint() {
        Integer tableCount = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN ('COMPANY','FILING','FILING_DOCUMENT','AI_ANALYSIS')", Integer.class);
        Integer uniqueCount = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name='FILING' AND constraint_type='UNIQUE'", Integer.class);
        assertThat(tableCount).isEqualTo(4);
        assertThat(uniqueCount).isGreaterThanOrEqualTo(1);
    }
}
