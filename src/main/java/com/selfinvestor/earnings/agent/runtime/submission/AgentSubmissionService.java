package com.selfinvestor.earnings.agent.runtime.submission;

import com.selfinvestor.earnings.agent.runtime.thread.AgentThread;
import com.selfinvestor.earnings.agent.runtime.thread.SubjectType;
import com.selfinvestor.earnings.agent.runtime.thread.ThreadManager;
import com.selfinvestor.earnings.enums.FilingStatus;
import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.repository.FilingRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将财报事件和用户输入统一包装为 Submission。
 */
@Service
public class AgentSubmissionService {
    @Resource
    private ThreadManager threads;
    @Resource
    private SubmissionQueue queue;
    @Resource
    private FilingRepository filings;

    /**
     * 为已解析的 Filing 创建 EARNINGS_RELEASED Submission。
     *
     * @param filingId Filing 主键
     * @return 已入队的 Submission
     */
    public Submission submitEarnings(Long filingId) {
        FilingPO filing = filings.findOneById(filingId).orElseThrow();
        AgentThread thread = threads.getOrCreate(SubjectType.STOCK, filing.getCompany().getTicker());
        EarningsReleasedInput input = new EarningsReleasedInput(
                filing.getCompany().getTicker(),
                filing.getCompany().getCompanyName(),
                filing.getId(),
                filing.getFormType(),
                filing.getFilingDate().toString(),
                filing.getSecUrl());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("filingId", filingId);
        Submission submission = Submission.builder()
                .threadId(thread.getId())
                .type(SubmissionType.EARNINGS_RELEASED)
                .input(input)
                .goal(earningsGoal(filing))
                .metadata(metadata)
                .build();
        filings.updateStatus(filingId, FilingStatus.ANALYZING, null);
        queue.submit(submission);
        return submission;
    }

    /**
     * 为股票 Thread 提交一个新的用户研究问题。
     *
     * @param ticker 股票代码
     * @param goal 研究目标
     * @param input 用户输入
     * @return 已入队的 Submission
     */
    public Submission submitUserInput(String ticker, String goal, String input) {
        return submit(ticker, SubmissionType.USER_INPUT, goal, input);
    }

    /**
     * 向股票 Thread 当前 Turn 追加引导输入。
     *
     * @param ticker 股票代码
     * @param input 引导输入
     * @return 已入队的 Submission
     */
    public Submission steer(String ticker, String input) {
        return submit(ticker, SubmissionType.STEER, input, input);
    }

    /**
     * 请求取消股票 Thread 当前 Turn。
     *
     * @param ticker 股票代码
     * @return 已入队的 Submission
     */
    public Submission cancel(String ticker) {
        return submit(ticker, SubmissionType.CANCEL, "Cancel active turn", "Cancel active turn");
    }

    /**
     * 创建并入队普通股票 Submission。
     *
     * @param ticker 股票代码
     * @param type Submission 类型
     * @param goal Turn 目标
     * @param input 输入文本
     * @return 已入队的 Submission
     */
    private Submission submit(String ticker, SubmissionType type, String goal, String input) {
        AgentThread thread = threads.getOrCreate(SubjectType.STOCK, ticker);
        Submission submission = Submission.builder()
                .threadId(thread.getId())
                .type(type)
                .input(new TextTurnInput(input))
                .goal(goal)
                .build();
        queue.submit(submission);
        return submission;
    }

    /**
     * 根据 Filing 创建财报研究目标。
     *
     * @param filing Filing 实体
     * @return 研究目标
     */
    private String earningsGoal(FilingPO filing) {
        return filing.getCompany().getTicker() + " released " + filing.getFormType() + ". "
                + "Analyze whether fundamentals materially changed. Focus on revenue growth, margins, segments, "
                + "free cash flow, capex, management guidance and changes versus previous periods. "
                + "Use only necessary tools and clearly separate facts from inference.";
    }
}
