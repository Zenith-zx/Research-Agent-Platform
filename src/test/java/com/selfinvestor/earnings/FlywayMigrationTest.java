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
        Integer tableCount = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN ('COMPANY','FILING','FILING_DOCUMENT','AI_ANALYSIS','MARKET_STRESS_ANALYSIS','MARKET_DAILY_CLOSE','EARNINGS_AGENT_RUN','AGENT_THREAD','AGENT_TURN','AGENT_TURN_ITEM')", Integer.class);
        Integer uniqueCount = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name='FILING' AND constraint_type='UNIQUE'", Integer.class);
        Integer closeUniqueCount = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name='MARKET_DAILY_CLOSE' AND constraint_type='UNIQUE'", Integer.class);
        assertThat(tableCount).isEqualTo(10);
        Integer macroTables = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_name IN ('MACRO_STATE','MACRO_ANALYSIS','MACRO_RELEASE')", Integer.class);
        assertThat(macroTables).isEqualTo(3);
        assertThat(uniqueCount).isGreaterThanOrEqualTo(1);
        assertThat(closeUniqueCount).isGreaterThanOrEqualTo(1);
    }
}
