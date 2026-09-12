package com.selfinvestor.earnings.service;

import com.selfinvestor.earnings.config.EarningsProperties;
import com.selfinvestor.earnings.enums.FilingStatus;
import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.repository.FilingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FilingRecoveryServiceTest {

    /**
     * 验证恢复服务从各稳定状态继续执行对应处理步骤。
     */
    @Test
    void resumesEachStablePipelineStatus() {
        FilingRepository repository = mock(FilingRepository.class);
        FilingProcessingService processingService = mock(FilingProcessingService.class);
        AiAnalysisService analysisService = mock(AiAnalysisService.class);
        EarningsProperties properties = new EarningsProperties();
        properties.getMonitor().setFilingLookbackDays(30);
        properties.getMonitor().setRecoveryBatchSize(20);

        FilingPO detected = filing(1L, FilingStatus.DETECTED);
        FilingPO downloaded = filing(2L, FilingStatus.DOWNLOADED);
        FilingPO parsed = filing(3L, FilingStatus.PARSED);
        FilingPO downloading = filing(4L, FilingStatus.DOWNLOADING);
        FilingPO parsing = filing(5L, FilingStatus.PARSING);
        FilingPO analyzing = filing(6L, FilingStatus.ANALYZING);
        when(repository.findRecoverable(any(LocalDate.class), eq(20)))
                .thenReturn(Arrays.asList(detected, downloaded, parsed, downloading, parsing, analyzing));

        FilingRecoveryService service = new FilingRecoveryService();
        ReflectionTestUtils.setField(service, "filingRepository", repository);
        ReflectionTestUtils.setField(service, "filingProcessingService", processingService);
        ReflectionTestUtils.setField(service, "aiAnalysisService", analysisService);
        ReflectionTestUtils.setField(service, "properties", properties);

        service.recoverIncompleteFilings();

        verify(processingService).download(1L);
        verify(processingService).parse(2L);
        verify(analysisService).analyze(3L);
        verify(processingService).download(4L);
        verify(processingService).parse(5L);
        verify(analysisService).analyze(6L);
    }

    /**
     * 创建指定状态的测试 Filing。
     *
     * @param id Filing 主键
     * @param status Filing 状态
     * @return 测试 Filing
     */
    private FilingPO filing(Long id, FilingStatus status) {
        FilingPO filing = new FilingPO();
        filing.setId(id);
        filing.setStatus(status);
        return filing;
    }
}
