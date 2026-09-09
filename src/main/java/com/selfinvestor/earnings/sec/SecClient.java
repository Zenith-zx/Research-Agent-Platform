package com.selfinvestor.earnings.sec;

import com.selfinvestor.earnings.company.CompanyPO;
import com.selfinvestor.earnings.sec.dto.SecFilingMetadataDTO;
import java.util.List;

public interface SecClient {
    /**
     * 查询公司的最近 SEC Filing。
     *
     * @param company 关注公司
     * @return Filing 元数据列表
     */
    List<SecFilingMetadataDTO> recentFilings(CompanyPO company);
}
