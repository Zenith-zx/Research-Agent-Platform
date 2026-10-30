package com.selfinvestor.earnings.controller;

import com.selfinvestor.earnings.agent.runtime.submission.AgentSubmissionService;
import com.selfinvestor.earnings.agent.runtime.submission.Submission;
import com.selfinvestor.earnings.controller.dto.AgentInputRequestDTO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 动态 Investment Agent 的用户输入、Steer 和 Cancel 接口。
 */
@RestController
@RequestMapping("/api/agent/threads/{ticker}")
@ConditionalOnProperty(prefix = "self-investor.agent", name = "enabled", havingValue = "true")
public class AgentController {
    @Resource
    private AgentSubmissionService submissions;

    /**
     * 为 ticker 启动新 Turn，已有活跃 Turn 时自动转为 Steer。
     *
     * @param ticker 股票代码
     * @param request 用户输入
     * @return Submission 回执
     */
    @PostMapping("/turns")
    public Map<String, Object> submit(@PathVariable String ticker,
                                      @Valid @RequestBody AgentInputRequestDTO request) {
        String goal = request.getGoal() == null || request.getGoal().isBlank()
                ? request.getText() : request.getGoal();
        return receipt(submissions.submitUserInput(ticker, goal, request.getText()));
    }

    /**
     * 向 ticker 当前活跃 Turn 追加输入。
     *
     * @param ticker 股票代码
     * @param request 引导输入
     * @return Submission 回执
     */
    @PostMapping("/steer")
    public Map<String, Object> steer(@PathVariable String ticker,
                                     @Valid @RequestBody AgentInputRequestDTO request) {
        return receipt(submissions.steer(ticker, request.getText()));
    }

    /**
     * 协作式取消 ticker 当前活跃 Turn。
     *
     * @param ticker 股票代码
     * @return Submission 回执
     */
    @PostMapping("/cancel")
    public Map<String, Object> cancel(@PathVariable String ticker) {
        return receipt(submissions.cancel(ticker));
    }

    /**
     * 创建统一的异步 Submission 回执。
     *
     * @param submission 已入队 Submission
     * @return 回执字段
     */
    private Map<String, Object> receipt(Submission submission) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("submissionId", submission.getSubmissionId());
        response.put("threadId", submission.getThreadId());
        response.put("type", submission.getType());
        response.put("accepted", true);
        return response;
    }
}
