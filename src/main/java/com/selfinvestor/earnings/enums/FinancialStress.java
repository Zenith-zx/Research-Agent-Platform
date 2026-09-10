package com.selfinvestor.earnings.enums;

import lombok.Getter;

/**
 * 金融系统压力等级。
 */
@Getter
public enum FinancialStress {
    LOW("金融系统压力低"), MODERATE("金融系统压力中等"), HIGH("金融系统压力高"), SYSTEMIC("系统性金融压力"), UNKNOWN("信息不足");

    /**
     * 压力描述。
     */
    private final String desc;

    /**
     * 创建金融压力等级。
     *
     * @param desc 压力描述
     */
    FinancialStress(String desc) {
        this.desc = desc;
    }
}
