package com.selfinvestor.earnings.agent.earnings.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 财报研究任务的业务分类。
 */
@Getter
@AllArgsConstructor
public enum ResearchCategory {
    REVENUE("研究收入规模、增长率、增长质量及其同比或环比变化"),
    EPS("研究每股收益、盈利变化及一次性因素的影响"),
    MARGIN("研究毛利率、营业利润率及成本费用结构变化"),
    CASH_FLOW("研究经营现金流、自由现金流、资本开支和现金转换质量"),
    GUIDANCE("研究公司对下一季度或全年的业绩指引及其变化"),
    SEGMENT("研究各业务分部、地区或产品线的收入和增长表现"),
    EXPECTATION("研究财报发布前的一致预期、市场预期以及 Beat/Miss"),
    MANAGEMENT_COMMENTARY("研究管理层对经营驱动、风险和未来趋势的表述"),
    OTHER("不属于以上标准分类但可能实质影响财报结论的研究问题");

    /**
     * 研究分类说明。
     */
    private final String desc;
}
