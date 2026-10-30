package com.selfinvestor.earnings.agent.macro;

import com.selfinvestor.earnings.agent.runtime.thread.SubjectType;
import com.selfinvestor.earnings.agent.runtime.turn.TurnItemService;
import com.selfinvestor.earnings.agent.runtime.turn.TurnItemType;
import com.selfinvestor.earnings.config.MacroAgentProperties;
import com.selfinvestor.earnings.event.AgentTurnCompletedEvent;
import com.selfinvestor.earnings.service.interfaces.NotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 在共享 Runtime 完成事务提交后发送通知；通知故障不回滚分析或宏观状态。
 */
@Component
@Slf4j
public class MacroNotificationListener {
    @Resource
    private MacroAgentProperties properties;
    @Resource
    private List<NotificationService> notifications;
    @Resource
    private TurnItemService items;

    /**
     * 复用配置好的通知渠道，默认关闭宏观推送。
     */
    @EventListener
    public void completed(AgentTurnCompletedEvent event) {
        if (event.getThread().getSubjectType() != SubjectType.MACRO || !properties.isNotificationsEnabled()) {
            return;
        }
        for (NotificationService channel : notifications) {
            try {
                channel.sendMacro(event.getTurn().getId(), event.getTurn().getFinalResponse());
            } catch (RuntimeException exception) {
                log.warn("Macro notification failed: turn={} channel={}", event.getTurn().getId(),
                        channel.getClass().getSimpleName(), exception);
                try {
                    items.append(event.getTurn().getId(), TurnItemType.ERROR,
                            "Notification failed: " + channel.getClass().getSimpleName(), null, null, null);
                } catch (RuntimeException auditFailure) {
                    log.error("Cannot record notification failure", auditFailure);
                }
            }
        }
    }
}
