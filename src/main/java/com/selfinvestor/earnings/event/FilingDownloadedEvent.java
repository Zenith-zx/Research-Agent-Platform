package com.selfinvestor.earnings.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Filing 下载完成事件。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilingDownloadedEvent {
    /**
     * Filing 主键。
     */
    private Long filingId;
}
