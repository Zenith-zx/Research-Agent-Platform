package com.selfinvestor.earnings.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filing 解析完成事件。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilingParsedEvent {
    /**
     * Filing 主键。
     */
    private Long filingId;
}
