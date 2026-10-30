package com.selfinvestor.earnings.agent.macro.model;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 带来源的发布事实。V1 由人工或上游采集器导入，不冒充自动官方采集。
 * CPI 使用 headlineYoy/headlineMom/coreYoy/coreMom；
 * NFP 使用 payrolls/unemployment/averageHourlyEarnings/participation/revision。
 */
@Data
public class MacroRelease {
    @NotBlank @Pattern(regexp = "[A-Za-z0-9._:-]{1,100}")
    private String releaseId;
    @NotBlank @Pattern(regexp = "CPI|NFP|FOMC|PCE|PMI|JOBLESS_CLAIMS")
    private String indicator;
    @NotBlank @Size(max = 32)
    private String period;
    @NotNull
    private OffsetDateTime releaseDate;
    @NotBlank @Size(max = 500)
    private String source;
    @Size(max = 2000)
    private String sourceUrl;
    @Size(max = 40)
    private Map<String, @Valid Reading> readings = new LinkedHashMap<>();
    @Size(max = 40)
    private Map<String, @Valid Reading> components = new LinkedHashMap<>();
    @Size(max = 2000)
    private String rateDecision;
    @Size(max = 20000)
    private String statement;
    @Size(max = 20000)
    private String previousStatement;
    @Size(max = 12000)
    private String projections;
    @Size(max = 12000)
    private String powellRemarks;

    /**
     * 单个指标的实际、预期、前值和修订；缺失值必须为 null，不能填零。
     * consensusSource 独立标注预期来源，unit 例如 % YoY / % MoM / thousand persons。
     */
    @Data
    public static class Reading {
        private BigDecimal actual;
        private BigDecimal consensus;
        private BigDecimal previous;
        private BigDecimal revision;
        @NotBlank @Size(max = 80)
        private String unit;
        @Size(max = 500)
        private String consensusSource;
    }
}
