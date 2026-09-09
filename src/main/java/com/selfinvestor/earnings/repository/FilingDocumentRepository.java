package com.selfinvestor.earnings.repository;

import com.selfinvestor.earnings.filing.FilingDocumentPO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface FilingDocumentRepository {
    /**
     * 查询 Filing 的全部附件。
     *
     * @param filingId Filing 主键
     * @return 附件列表
     */
    List<FilingDocumentPO> findAllByFilingIdOrderById(Long filingId);
    /**
     * 查询被选中用于分析的附件。
     *
     * @param filingId Filing 主键
     * @return 附件列表
     */
    List<FilingDocumentPO> findAllByFilingIdAndSelectedForAnalysisTrue(Long filingId);
    /**
     * 新增 Filing 附件。
     *
     * @param document 附件实体
     * @return 影响行数
     */
    int insert(FilingDocumentPO document);
    /**
     * 更新附件解析结果。
     *
     * @param document 附件实体
     * @return 影响行数
     */
    int updateParsed(FilingDocumentPO document);
}
