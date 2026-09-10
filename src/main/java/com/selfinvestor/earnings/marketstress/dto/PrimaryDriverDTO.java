package com.selfinvestor.earnings.marketstress.dto;

import com.selfinvestor.earnings.enums.Importance;
import com.selfinvestor.earnings.enums.ShockType;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 市场主要驱动因素 DTO。
 */
@Data
@NoArgsConstructor
public class PrimaryDriverDTO {
    /**
     * 冲击类型。
     */
    private ShockType type;
    /**
     * 重要程度。
     */
    private Importance importance;
    /**
     * 基于输入事实的驱动说明。
     */
    private String description;
}
