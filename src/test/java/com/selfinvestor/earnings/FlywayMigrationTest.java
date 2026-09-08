package com.selfinvestor.earnings;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flyway;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none"
})
class FlywayMigrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void migrationCreatesCoreTablesAndAccessionUniqueConstraint() {
        Integer tableCount = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN ('company','filing','filing_document','ai_analysis')", Integer.class);
        Integer uniqueCount = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name='filing' AND constraint_type='UNIQUE'", Integer.class);
        assertThat(tableCount).isEqualTo(4);
        assertThat(uniqueCount).isGreaterThanOrEqualTo(1);
    }
}
