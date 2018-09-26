package com.mash.api.service;

import com.mash.api.entity.ProductPeriod;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 排期管理service
 */
public interface ProductPeriodService {

    /**
     * 查询单个广告位所有排期
     * @param productId
     * @return
     */
    List<ProductPeriod> findByProductId(Integer productId);

    List<ProductPeriod> findAll();

    ProductPeriod save(ProductPeriod productPeriod);

    /**
     * 更新排期状态
     * @param id
     * @param state
     */
    void updateState(Integer id, Integer state);

    /**
     * 根据项目ID查询排期
     * @param scheduleId
     * @return
     */
    List<ProductPeriod> findByScheduleId(Integer scheduleId);

    /**
     * 设置正面图片
     * @param filePath
     * @param idList
     */
    void setPositivePicture(String filePath, List<Integer> idList);

    /**
     * 设置背面图片
     * @param filePath
     * @param idList
     */
    void setBackPicture(String filePath, List<Integer> idList);

    ProductPeriod findById(Integer id);

    /**
     * 批量分配安装工人
     * @param worker
     * @param workerAccountId
     * @param idList
     */
    void multipleSetWorker(String worker, Integer workerAccountId, List<Integer> idList);

    /**
     * 批量设置拆除工人
     * @param worker
     * @param workerAccountId
     * @param idList
     * @param request
     */
    void multipleSetDismantleWorker(String worker, Integer workerAccountId, List<Integer> idList, HttpServletRequest request);

    /**
     * 删除根据ID
     * @param id
     */
    void deleteById(Integer id);
}
