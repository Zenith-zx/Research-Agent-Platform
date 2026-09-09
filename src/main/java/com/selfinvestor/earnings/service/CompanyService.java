package com.selfinvestor.earnings.service;

import com.selfinvestor.earnings.common.exception.BusinessException;
import com.selfinvestor.earnings.company.CompanyPO;
import com.selfinvestor.earnings.repository.CompanyRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompanyService {

    @Resource
    private CompanyRepository repository;

    /**
     * 查询所有启用监听的公司。
     *
     * @return 已启用公司列表
     */
    @Transactional(readOnly = true)
    public List<CompanyPO> enabledCompanies() {
        return repository.findAllByEnabledTrueOrderByTicker();
    }

    /**
     * 查询全部关注公司。
     *
     * @return 公司列表
     */
    @Transactional(readOnly = true)
    public List<CompanyPO> allCompanies() {
        return repository.findAll();
    }

    /**
     * 新增关注公司。
     *
     * @param ticker 股票代码
     * @param cik SEC CIK
     * @param companyName 公司名称
     * @param enabled 是否启用监听
     * @return 新增公司
     */
    @Transactional
    public CompanyPO add(String ticker, String cik, String companyName, boolean enabled) {
        String normalizedTicker = ticker.trim().toUpperCase();
        String normalizedCik = normalizeCik(cik);
        repository.findByTickerIgnoreCase(normalizedTicker).ifPresent(existing -> {
            throw new BusinessException("Ticker already exists: " + normalizedTicker);
        });
        CompanyPO company = new CompanyPO(normalizedTicker, normalizedCik, companyName.trim(), enabled);
        repository.insert(company);
        return company;
    }

    /**
     * 修改公司的监听状态。
     *
     * @param id 公司主键
     * @param enabled 是否启用监听
     * @return 修改后的公司
     */
    @Transactional
    public CompanyPO setEnabled(Long id, boolean enabled) {
        CompanyPO company = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Company not found: " + id));
        company.setEnabled(enabled);
        repository.updateEnabled(id, enabled);
        return company;
    }

    /**
     * 将 CIK 规范化为十位数字。
     *
     * @param cik 原始 CIK
     * @return 十位 CIK
     */
    static String normalizeCik(String cik) {
        String digits = cik == null ? "" : cik.replaceAll("\\D", "");
        if (digits.isBlank() || digits.length() > 10) {
            throw new BusinessException("CIK must contain 1 to 10 digits");
        }
        return String.format("%010d", Long.parseLong(digits));
    }

    /**
     * 按股票代码查询关注公司。
     *
     * @param ticker 股票代码
     * @return 公司实体
     */
    @Transactional(readOnly = true)
    public CompanyPO findByTicker(String ticker) {
        return repository.findByTickerIgnoreCase(ticker)
                .orElseThrow(() -> new BusinessException("Unknown ticker: " + ticker));
    }
}
