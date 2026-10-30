package com.selfinvestor.earnings.agent.runtime.submission;

import java.time.Duration;

/**
 * Submission 生产者与消费者之间的队列抽象。
 */
public interface SubmissionQueue {
    /**
     * 将外部输入提交到队列；队列满时施加背压。
     *
     * @param submission 外部输入
     */
    void submit(Submission submission);

    /**
     * 在指定时间内等待下一条 Submission。
     *
     * @param timeout 最长等待时间
     * @return Submission；超时时为空
     */
    Submission poll(Duration timeout);
}
