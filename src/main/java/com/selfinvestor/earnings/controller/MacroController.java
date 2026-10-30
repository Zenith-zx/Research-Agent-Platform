package com.selfinvestor.earnings.controller;

import com.selfinvestor.earnings.agent.macro.*;
import com.selfinvestor.earnings.agent.macro.model.*;
import com.selfinvestor.earnings.agent.runtime.submission.*;
import com.selfinvestor.earnings.agent.runtime.thread.*;
import com.selfinvestor.earnings.agent.runtime.turn.*;
import com.selfinvestor.earnings.controller.dto.AgentInputRequestDTO;
import com.selfinvestor.earnings.repository.AgentTurnRepository;
import com.selfinvestor.earnings.repository.MacroRepository;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

/**
 * US_MACRO 人工事实入口与只读审计接口，不提供直接写 regime 的 API。
 */
@RestController
@RequestMapping("/api/macro")
@ConditionalOnProperty(prefix = "self-investor.macro", name = "enabled", havingValue = "true")
public class MacroController {
    @Resource
    private MacroTrigger trigger;
    @Resource
    private MacroReleaseService releases;
    @Resource
    private MacroStateService states;
    @Resource
    private ThreadManager threads;
    @Resource
    private AgentTurnRepository turns;
    @Resource
    private TurnItemService items;
    @Resource
    private MacroRepository repository;

    /**
     * 导入历史事实，不触发模型；便于先建立趋势档案再分析新发布。
     */
    @PostMapping("/releases")
    public Map<String, Object> importRelease(@Valid @RequestBody MacroRelease release) {
        releases.store(release);
        return Map.of("releaseId", release.getReleaseId(), "stored", true, "analysisTriggered", false);
    }

    /**
     * 保存 CPI/NFP/FOMC 事实并投递 Submission；只返回已接受，不代表分析完成。
     */
    @PostMapping("/events/{type}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> event(@PathVariable SubmissionType type, @Valid @RequestBody MacroRelease release) {
        return receipt(trigger.published(type, release));
    }

    /**
     * 用户启动宏观研究，已有活跃 Turn 时追加为 Steer。
     */
    @PostMapping("/input")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> input(@Valid @RequestBody AgentInputRequestDTO request) {
        return receipt(trigger.input(request.getText(), false));
    }

    /**
     * 向正在分析的 US_MACRO Turn 追加问题。
     */
    @PostMapping("/steer")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> steer(@Valid @RequestBody AgentInputRequestDTO request) {
        return receipt(trigger.input(request.getText(), true));
    }

    /**
     * 协作式取消当前宏观任务。
     */
    @PostMapping("/cancel")
    public Map<String, Object> cancel() {
        return receipt(trigger.cancel());
    }

    /**
     * 查询唯一长期宏观 Thread。
     */
    @GetMapping("/thread")
    public AgentThread thread() {
        return threads.getOrCreate(SubjectType.MACRO, "US_MACRO");
    }

    /**
     * 查询最新结构化状态及版本。
     */
    @GetMapping("/state")
    public MacroState state() {
        return states.getCurrentUsMacroState();
    }

    /**
     * 查询最近二十次宏观 Turn，包含状态、失败原因和最终报告。
     */
    @GetMapping("/turns")
    public List<AgentTurn> turns() {
        return turns.findRecentByThreadId(thread().getId());
    }

    /**
     * 按序查询宏观 Turn 的模型决策、工具调用、观察和 Steer。
     */
    @GetMapping("/turns/{turnId}/items")
    public List<AgentTurnItem> items(@PathVariable String turnId) {
        requireMacroTurn(turnId);
        return items.history(turnId);
    }

    /**
     * 查询已完成宏观 Turn 的结构化结果，未完成返回 404。
     */
    @GetMapping("/turns/{turnId}/analysis")
    public MacroRecord analysis(@PathVariable String turnId) {
        requireMacroTurn(turnId);
        return repository.findAnalysis(turnId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /**
     * 禁止通过宏观接口读取其他领域的 Turn。
     */
    private void requireMacroTurn(String turnId) {
        AgentTurn turn = turns.findById(turnId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!turn.getThreadId().equals(thread().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * 返回共享 Submission 的异步回执。
     */
    private Map<String, Object> receipt(Submission submission) {
        return Map.of("accepted", true, "submissionId", submission.getSubmissionId(),
                "threadId", submission.getThreadId(), "type", submission.getType());
    }

    /**
     * 将无效事实/事件参数反馈为 400，而不是误报服务故障。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalid(IllegalArgumentException exception) {
        return Map.of("error", exception.getMessage());
    }
}
