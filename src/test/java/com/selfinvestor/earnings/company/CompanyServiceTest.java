package com.selfinvestor.earnings.company;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {
    @Mock CompanyRepository repository;

    @Test
    void normalizesTickerAndCikWhenAddingCompany() {
        when(repository.findByTickerIgnoreCase("NVDA")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Company result = new CompanyService(repository).add("nvda", "1045810", "NVIDIA", true);
        assertThat(result.getTicker()).isEqualTo("NVDA");
        assertThat(result.getCik()).isEqualTo("0001045810");
    }
}
