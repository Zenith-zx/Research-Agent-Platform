package com.selfinvestor.earnings.company;

import com.selfinvestor.earnings.repository.CompanyRepository;
import com.selfinvestor.earnings.service.CompanyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyPOServiceTest {
    @Mock CompanyRepository repository;

    /**
     * 验证新增公司时会规范化股票代码和 CIK。
     */
    @Test
    void normalizesTickerAndCikWhenAddingCompany() {
        when(repository.findByTickerIgnoreCase("NVDA")).thenReturn(Optional.empty());
        CompanyService service = new CompanyService();
        ReflectionTestUtils.setField(service, "repository", repository);
        CompanyPO result = service.add("nvda", "1045810", "NVIDIA", true);
        assertThat(result.getTicker()).isEqualTo("NVDA");
        assertThat(result.getCik()).isEqualTo("0001045810");
        verify(repository).insert(result);
    }
}
