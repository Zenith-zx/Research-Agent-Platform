package com.selfinvestor.earnings.enums;

import lombok.Getter;

/**
 * 卖方衰竭强度。
 */
@Getter
public enum SellerExhaustion {
    NONE("未发现卖方衰竭"), WEAK("弱卖方衰竭"), MODERATE("中等卖方衰竭"), STRONG("强卖方衰竭"), UNKNOWN("信息不足");

    /**
     * 强度描述。
     */
    private final String desc;

    /**
     * 创建卖方衰竭强度。
     *
     * @param desc 强度描述
     */
    SellerExhaustion(String desc) {
        this.desc = desc;
    }
}
