package com.selfinvestor.earnings.service;

import com.selfinvestor.earnings.company.CompanyPO;
import com.selfinvestor.earnings.event.FilingDetectedEvent;
import com.selfinvestor.earnings.filing.FilingPO;
import com.selfinvestor.earnings.repository.FilingRepository;
import com.selfinvestor.earnings.sec.dto.SecFilingMetadataDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class FilingService {

    @Resource
    private FilingRepository repository;

    @Resource
    private ApplicationEventPublisher events;

    @Resource
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactions;

    /**
     * 初始化事务模板。
     */
    @PostConstruct
    void initialize() {
        transactions = new TransactionTemplate(transactionManager);
    }

    /**
     * 注册一份尚未处理的 Filing。
     *
     * @param company 关联公司
     * @param metadata SEC Filing 元数据
     * @return 新增 Filing；重复时为空
     */
    public Optional<FilingPO> registerIfNew(CompanyPO company, SecFilingMetadataDTO metadata) {
        if (repository.existsByAccessionNumber(metadata.getAccessionNumber())) {
            return Optional.empty();
        }
        try {
            FilingPO filing = transactions.execute(status -> {
                FilingPO saved = new FilingPO(company, metadata.getAccessionNumber(), metadata.getFormType(),
                        metadata.getFilingDate(), metadata.getAcceptedAt(), metadata.getPrimaryDocument(), metadata.getFilingUrl());
                repository.insert(saved);
                events.publishEvent(new FilingDetectedEvent(saved.getId()));
                return saved;
            });
            log.info("Filing detected: {} {} accession={}", company.getTicker(), metadata.getFormType(), metadata.getAccessionNumber());
            return Optional.ofNullable(filing);
        } catch (DataIntegrityViolationException duplicate) {
            log.debug("Duplicate filing ignored: accession={}", metadata.getAccessionNumber());
            return Optional.empty();
        }
    }

    /**
     * 按主键倒序查询全部 Filing。
     *
     * @return Filing 列表
     */
    public List<FilingPO> findAll() {
        return repository.findAllByOrderByIdDesc();
    }
}
