package com.selfinvestor.earnings.agent.macro;

import com.selfinvestor.earnings.agent.macro.model.MacroRelease;
import com.selfinvestor.earnings.agent.runtime.submission.*;
import com.selfinvestor.earnings.agent.runtime.thread.*;
import com.selfinvestor.earnings.config.AgentRuntimeProperties;
import com.selfinvestor.earnings.config.MacroAgentProperties;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.Map;

/**
 * 事件输入适配层：只记录发生了什么、设置研究目标并入队，不做分析。
 */
@Service
public class MacroTrigger {
    @Resource
    private ThreadManager threads;
    @Resource
    private SubmissionQueue queue;
    @Resource
    private MacroReleaseService releases;
    @Resource
    private MacroStateService states;
    @Resource
    private MacroAgentProperties properties;
    @Resource
    private AgentRuntimeProperties runtimeProperties;

    /**
     * CPI/NFP/FOMC 事实发布入口，可由 HTTP 或未来官方监听器调用。
     */
    public Submission published(SubmissionType type, MacroRelease release) {
        requireEnabled();
        String expected = switch (type) {
            case CPI_RELEASED -> "CPI";
            case NFP_RELEASED -> "NFP";
            case FOMC_RELEASED -> "FOMC";
            default -> throw new IllegalArgumentException("V1 supports CPI_RELEASED, NFP_RELEASED, FOMC_RELEASED only");
        };
        if (!expected.equals(release.getIndicator())) {
            throw new IllegalArgumentException("Trigger type does not match release indicator");
        }
        releases.store(release);
        return submit(type, goal(type), releases.json(release), Map.of("releaseId", release.getReleaseId()));
    }

    /**
     * 用户研究或 Steer 统一进入 US_MACRO，已有活跃 Turn 时追加同一 Turn。
     */
    public Submission input(String text, boolean steer) {
        requireEnabled();
        if (text == null || text.isBlank() || text.length() > 12000) {
            throw new IllegalArgumentException("Macro input must contain 1-12000 characters");
        }
        return submit(steer ? SubmissionType.STEER : SubmissionType.MACRO_USER_INPUT,
                text, text, Map.of());
    }

    /**
     * 提交协作式取消，不修改宏观状态。
     */
    public Submission cancel() {
        requireEnabled();
        return submit(SubmissionType.CANCEL, "Cancel current macro turn", "", Map.of());
    }

    /**
     * 创建或复用唯一长期 Thread，并将纯事实 Submission 交给共享队列。
     */
    private Submission submit(SubmissionType type, String goal, String text, Map<String, Object> metadata) {
        AgentThread thread = threads.getOrCreate(SubjectType.MACRO, "US_MACRO");
        states.getOrCreate(thread.getId());
        Submission submission = Submission.builder().threadId(thread.getId()).type(type).goal(goal)
                .input(new TextTurnInput(text)).metadata(metadata).build();
        queue.submit(submission);
        return submission;
    }

    /**
     * 根据事件选择研究目标，而不是选择或固定工具调用顺序。
     */
    private String goal(SubmissionType type) {
        return switch (type) {
            case CPI_RELEASED -> "Analyze the new US CPI: actual vs consensus vs previous/trend, core/headline YoY/MoM "
                    + "and available composition. Assess inflation regime and Fed path, selectively inspect nominal/real yields, "
                    + "dollar and equities. Consider prior pricing; hotter is not automatically bearish. Propose only supported regime changes.";
            case NFP_RELEASED -> "Analyze US employment: payroll surprise, unemployment, wages, participation and revisions. "
                    + "Determine tight/balanced/cooling/weak labor and transmission to growth, inflation, Fed, rates and risk assets.";
            case FOMC_RELEASED -> "Analyze FOMC rate decision, statement changes, SEP/dot plot and Powell remarks if available. "
                    + "Compare prior guidance and market pricing; assess dovish through hawkish Fed regime and transmission "
                    + "to nominal/real yields, dollar, equities and risk appetite.";
            default -> throw new IllegalArgumentException("Unsupported macro goal");
        };
    }

    /**
     * 禁用时拒绝入队，避免没有消费者的静默挂起。
     */
    private void requireEnabled() {
        if (!properties.isEnabled() || !runtimeProperties.isEnabled()) {
            throw new IllegalStateException("Enable self-investor.agent.enabled and self-investor.macro.enabled first");
        }
    }
}
