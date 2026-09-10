package com.selfinvestor.earnings.event;

import com.selfinvestor.earnings.marketstress.dto.DailyBarDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 市场超卖被检测事件。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OversoldDetectedEvent {
    /**
     * 市场压力分析主键。
     */
    private Long analysisId;
    /**
     * 触发标的。
     */
    private String symbol;
    /**
     * 用于触发判断的历史日线。
     */
    private List<DailyBarDTO> dailyBars = new ArrayList<>();
}
