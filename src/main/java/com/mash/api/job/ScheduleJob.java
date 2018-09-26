package com.mash.api.job;

import com.mash.api.entity.ProductPeriod;
import com.mash.api.entity.Schedule;
import com.mash.api.service.ProductPeriodService;
import com.mash.api.service.ScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ScheduleJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduleJob.class);

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ProductPeriodService productPeriodService;

    /**
     * 如果超过锁定时间，则自动释放该广告位
     */
    @Scheduled(fixedRate = 5000)
    public void timerRate() {
        // 查询所有审核成功未锁定的广告位
        List<Schedule> scheduleList = scheduleService.findByState(2);

        for (Schedule schedule : scheduleList)
        {
            Integer lockDays=  schedule.getLockDays();
            // 审核时间
            Date examineTime = schedule.getExamineTime();
            Calendar examineCal= Calendar.getInstance();
            examineCal.setTime(examineTime);
            examineCal.add(Calendar.DATE, lockDays);
            // 当前时间
            Calendar currentCal = Calendar.getInstance();
            // 如果超过锁定时间
            if (examineCal.compareTo(currentCal) == -1)
            {
                // 释放排期单中所有广告位  未提交
                schedule.setState(0);
                scheduleService.save(schedule);
                // 更新排期单中所有广告位状态
                Set<ProductPeriod> productPeriods = schedule.getProductPeriods();
                Iterator<ProductPeriod> iterator = productPeriods.iterator();
                while(iterator.hasNext())
                {
                    ProductPeriod productPeriod = iterator.next();
                    productPeriodService.updateState(productPeriod.getId(), 0);
                }
            }

        }
    }
}
