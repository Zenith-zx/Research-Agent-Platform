package com.selfinvestor.earnings.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filing 被发现事件。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilingDetectedEvent {
    /**
     * Filing 主键。
     */
    private Long filingId;
}
