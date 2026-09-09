package com.selfinvestor.earnings.controller.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改启用状态请求 DTO。
 */
@Data
@NoArgsConstructor
public class EnabledRequestDTO {
    /**
     * 是否启用监听。
     */
    private boolean enabled;
}
