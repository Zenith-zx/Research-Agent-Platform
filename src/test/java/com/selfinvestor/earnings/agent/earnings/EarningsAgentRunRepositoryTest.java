package com.selfinvestor.earnings.agent.earnings;

import com.selfinvestor.earnings.agent.earnings.model.AgentStatus;
import com.selfinvestor.earnings.repository.EarningsAgentRunRepository;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EarningsAgentRunRepositoryTest {
    @Resource
    private JdbcTemplate jdbc;
    @Resource
    private EarningsAgentRunRepository runs;

    @Test
    void persistsAndUpdatesCheckpointState() {
        jdbc.update("INSERT INTO COMPANY (ticker, cik, company_name, enabled, created_at, updated_at) "
                        + "VALUES (?, ?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                "AGENT_TEST", "0099999998", "Agent Test");
        Long companyId = jdbc.queryForObject("SELECT id FROM COMPANY WHERE ticker='AGENT_TEST'", Long.class);
        jdbc.update("INSERT INTO FILING (company_id, accession_number, form_type, filing_date, sec_url, status, created_at, updated_at) "
                        + "VALUES (?, ?, '10-Q', CURRENT_DATE, 'url', 'ANALYZING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                companyId, "agent-test-accession");
        Long filingId = jdbc.queryForObject("SELECT id FROM FILING WHERE accession_number='agent-test-accession'", Long.class);

        EarningsAgentRunPO run = new EarningsAgentRunPO();
        run.setRunId("agent-run-test");
        run.setFilingId(filingId);
        run.setTicker("AGENT_TEST");
        run.setStatus(AgentStatus.INITIALIZED);
        run.setCurrentRound(1);
        run.setStartTime(Instant.now());
        run.setStateJson("{\"stepCount\":0}");
        runs.insert(run);

        run.setStatus(AgentStatus.RUNNING);
        run.setStepCount(1);
        run.setStateJson("{\"stepCount\":1}");
        runs.update(run);

        EarningsAgentRunPO saved = runs.findByRunId("agent-run-test").orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(AgentStatus.RUNNING);
        assertThat(saved.getStepCount()).isOne();
        assertThat(saved.getStateJson()).contains("\"stepCount\":1");
    }
}
