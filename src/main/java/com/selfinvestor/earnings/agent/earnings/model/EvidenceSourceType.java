package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 证据来源类型。
 */
@Getter
@AllArgsConstructor
public enum EvidenceSourceType {
    COMPANY_EARNINGS_RELEASE("公司正式发布的季度或年度业绩新闻稿"),
    SEC_FILING("提交给美国证券交易委员会的 10-Q、10-K、8-K 等法定文件"),
    EARNINGS_CALL("公司财报电话会的管理层陈述或问答记录"),
    INVESTOR_RELATIONS("公司投资者关系网站发布的演示材料或补充资料"),
    CONSENSUS("分析师一致预期或可验证的市场预期数据"),
    NEWS("可信新闻媒体或通讯社的报道"),
    OTHER("无法归入以上类别的其他证据来源");

    /**
     * 来源说明。
     */
    private final String desc;
}
