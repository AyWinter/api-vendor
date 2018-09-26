package com.mash.api.service;

import com.mash.api.entity.Schedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.List;

public interface ScheduleService {

    /**
     * 更新排期状态
     * @param id
     * @param state
     */
    void updateState(Integer id, Integer state, HttpServletRequest request);

    /**
     * 保存排期
     * @param schedule
     * @return
     */
    Schedule save(Schedule schedule);

    /**
     * 查询所有排期
     * @return
     */
    List<Schedule> findAll();

    /**
     * 排期单分页条件查询
     * @param specification
     * @param pageable
     * @return
     */
    Page<Schedule> findByParams(Specification<Schedule> specification, Pageable pageable);

    /**
     * 查询排期单根据Number
     * @param number
     * @return
     */
    Schedule findByNumber(String number);

    /**
     * 查询排期根据ID
     * @param id
     * @return
     */
    Schedule findById(Integer id);

    /**
     * 做成排期
     * @param projectId
     * @param productIds
     * @param startTime
     * @param endTime
     * @param amount
     * @param request
     * @return
     */
    Schedule makeSchedule(Integer projectId,
                          String productIds,
                          String startTime,
                          String endTime,
                          String amount,
                          HttpServletRequest request) throws Exception;

    /**
     * 重新设置排期（向排期中重新添加广告位）
     * @param scheduleId
     * @param startTime
     * @param endTime
     * @param productIds
     * @return
     */
    void resetSchedule(Integer scheduleId, String startTime, String endTime, String productIds) throws Exception;

    /**
     * 删除排期中的某个产品
     * @param periodId
     * @param productId
     * @param scheduleId
     */
    void deleteProductByScheduleI(Integer periodId, Integer productId, Integer scheduleId) throws Exception;

    List<Schedule> findByState(Integer state);
}
