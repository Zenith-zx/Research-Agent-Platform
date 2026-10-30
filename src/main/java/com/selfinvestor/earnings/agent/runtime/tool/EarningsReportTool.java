package com.selfinvestor.earnings.agent.runtime.tool;

import com.selfinvestor.earnings.filing.FilingContentProcessor;
import com.selfinvestor.earnings.filing.FilingDocumentPO;
import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.repository.FilingDocumentRepository;
import com.selfinvestor.earnings.repository.FilingRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 读取已下载、清洗并选中的真实 Filing 内容。
 */
@Component
public class EarningsReportTool implements AgentTool {
    @Resource
    private FilingRepository filings;
    @Resource
    private FilingDocumentRepository documents;
    @Resource
    private FilingContentProcessor processor;

    /**
     * 返回 Tool 名称。
     *
     * @return Tool 名称
     */
    @Override
    public String name() {
        return "get_earnings_report";
    }

    /**
     * 返回 Tool 能力说明。
     *
     * @return 能力说明
     */
    @Override
    public String description() {
        return "读取指定 filingId 已解析并选中用于分析的财报正文，包含财务报表、指引和管理层评论。";
    }

    /**
     * 返回 filingId 参数 Schema。
     *
     * @return 参数 Schema
     */
    @Override
    public ToolSchema schema() {
        return new ToolSchema(Map.of("filingId", "Filing 数据库主键；不传时使用当前 Submission 的 filingId"),
                List.of());
    }

    /**
     * 加载 Filing 元数据和语义内容块。
     *
     * @param context Agent 上下文
     * @param arguments Tool 参数
     * @return Filing 和正文块
     */
    @Override
    public Object execute(AgentToolContext context, Map<String, Object> arguments) {
        Long filingId = filingId(context, arguments);
        FilingPO filing = filings.findOneById(filingId)
                .orElseThrow(() -> new IllegalArgumentException("Filing not found: " + filingId));
        List<FilingDocumentPO> selected = documents.findAllByFilingIdAndSelectedForAnalysisTrue(filingId);
        List<Map<String, Object>> chunks = processor.process(selected).stream().map(chunk -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("type", chunk.getType().name());
            value.put("content", chunk.getContent());
            return value;
        }).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("filingId", filing.getId());
        result.put("ticker", filing.getCompany().getTicker());
        result.put("formType", filing.getFormType());
        result.put("filingDate", filing.getFilingDate());
        result.put("secUrl", filing.getSecUrl());
        result.put("chunks", chunks);
        return result;
    }

    /**
     * 从 Tool 参数或 Submission metadata 中读取 filingId。
     *
     * @param context Agent 上下文
     * @param arguments Tool 参数
     * @return Filing 主键
     */
    private Long filingId(AgentToolContext context, Map<String, Object> arguments) {
        Object value = arguments.get("filingId");
        if (value == null && context.getSubmission() != null) {
            value = context.getSubmission().getMetadata().get("filingId");
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null && !value.toString().isBlank()) {
            return Long.valueOf(value.toString());
        }
        throw new IllegalArgumentException("filingId is required");
    }
}
