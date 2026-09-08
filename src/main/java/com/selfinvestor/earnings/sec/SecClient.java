package com.selfinvestor.earnings.sec;

import com.selfinvestor.earnings.company.Company;
import com.selfinvestor.earnings.sec.dto.SecFilingMetadata;
import java.util.List;

public interface SecClient {
    List<SecFilingMetadata> recentFilings(Company company);
}
