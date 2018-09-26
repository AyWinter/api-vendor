package com.mash.api.service.impl;

import com.mash.api.entity.ProductPeriod;
import com.mash.api.repository.ProductPeriodRepository;
import com.mash.api.service.ProductPeriodService;
import com.mash.api.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Service
public class ProductPeriodServiceImpl implements ProductPeriodService {

    @Autowired
    private ProductPeriodRepository productPeriodRepository;

    @Autowired
    private ScheduleService scheduleService;

    @Override
    public List<ProductPeriod> findByProductId(Integer productId) {
        return productPeriodRepository.findByProductId(productId);
    }

    @Override
    public List<ProductPeriod> findAll() {
        return productPeriodRepository.findAll();
    }

    @Override
    public ProductPeriod save(ProductPeriod productPeriod) {
        return productPeriodRepository.save(productPeriod);
    }

    @Override
    public void updateState(Integer id, Integer state) {
        productPeriodRepository.updateState(id, state);
    }

    @Override
    public List<ProductPeriod> findByScheduleId(Integer scheduleId) {
        return productPeriodRepository.findByScheduleId(scheduleId);
    }

    @Override
    public void setPositivePicture(String filePath, List<Integer> idList) {
        productPeriodRepository.setPositivePicture(filePath, idList);
    }

    @Override
    public void setBackPicture(String filePath, List<Integer> idList) {
        productPeriodRepository.setBackPicture(filePath, idList);
    }

    @Override
    public ProductPeriod findById(Integer id) {
        return productPeriodRepository.findOne(id);
    }

    @Override
    public void multipleSetWorker(String worker, Integer workerAccountId, List<Integer> idList) {
        productPeriodRepository.multipleSetWorker(worker, workerAccountId, idList);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void multipleSetDismantleWorker(String worker,
                                           Integer workerAccountId,
                                           List<Integer> idList,
                                           HttpServletRequest request) {
        // 更新产品状态 ：  已结束
        productPeriodRepository.multipleSetDismantleWorker(worker, workerAccountId, idList);
        // 更新排期单状态：已结束
        Integer id = idList.get(0);
        ProductPeriod productPeriod = productPeriodRepository.findOne(id);
        // 排期单ID
        Integer scheduleId = productPeriod.getSchedule().getId();

        scheduleService.updateState(scheduleId, 6, request);
    }

    @Override
    public void deleteById(Integer id) {

        productPeriodRepository.delete(id);
    }
}
