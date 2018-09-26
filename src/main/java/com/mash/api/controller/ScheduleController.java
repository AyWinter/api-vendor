package com.mash.api.controller;

import com.mash.api.entity.ProductPeriod;
import com.mash.api.entity.Result;
import com.mash.api.entity.Schedule;
import com.mash.api.service.*;
import com.mash.api.util.ResultUtil;
import com.mash.api.util.Tools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * 排期单 controller
 */
@RestController
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private ProductPeriodService productPeriodService;
    @Autowired
    private EnterpriseService enterpriseService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private EmployeeService employeeService;


    @GetMapping(value="/vendor/schedule")
    public Result<Schedule> findAll()
    {
        List<Schedule> schedules = scheduleService.findAll();

        return ResultUtil.success(schedules);
    }

    @PostMapping(value="/vendor/schedule")
    public Result<Schedule> findByParams(@RequestParam("state")Integer state,
                                         @RequestParam("page")Integer page,
                                         @RequestParam("pageSize")Integer pageSize,
                                         HttpServletRequest request)
    {
        Pageable pageable = new PageRequest(page,pageSize, Sort.Direction.DESC,"id");
        final Integer scheduleState = state;
        final Integer vendorId = Tools.getVendorId(request,
                enterpriseService,
                accountService,
                employeeService,
                departmentService);

        Specification<Schedule> specification = new Specification<Schedule>(){

            @Override
            public Predicate toPredicate(Root<Schedule> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicate = new ArrayList<>();

                if (scheduleState != -1)
                {
                    predicate.add(criteriaBuilder.equal(root.get("state").as(Integer.class), scheduleState));
                }
                // 查询未删除的排期单
                predicate.add(criteriaBuilder.notEqual(root.get("state").as(Integer.class), 7));
                predicate.add(criteriaBuilder.equal(root.get("vendorId").as(Integer.class), vendorId));

                Predicate[] pre = new Predicate[predicate.size()];
                return criteriaQuery.where(predicate.toArray(pre)).getRestriction();
            }
        };
        return ResultUtil.success(scheduleService.findByParams(specification, pageable));
    }

    /**
     * 查询排期单详细信息根据number
     * @param number
     * @return
     */
    @GetMapping(value="/vendor/schedule/{number}")
    public Result<Schedule> findByNumber(@PathVariable("number")String number)
    {
        return ResultUtil.success(scheduleService.findByNumber(number));
    }

    /**
     * 查询排期单详细信息根据id
     * @param id
     * @return
     */
    @GetMapping(value="/vendor/schedule/id/{id}")
    public Result<Schedule> findById(@PathVariable("id")Integer id)
    {
        return ResultUtil.success(scheduleService.findById(id));
    }

    /**
     * 更新排期单状态
     * @param id
     * @param state
     * @return
     */
    @PostMapping(value="/vendor/schedule/state")
    public Result updateState(@RequestParam("id")Integer id,
                              @RequestParam("state")Integer state,
                              HttpServletRequest request)
    {
        List<String> productName = new ArrayList<String>();
        if(state == 1)
        {
            // 验证排期是否已经被占用
            List<ProductPeriod> productPeriods = productPeriodService.findByScheduleId(id);

            for (ProductPeriod productPeriod : productPeriods)
            {
                Integer productId = productPeriod.getProduct().getId();
                // 查询当前产品的所有排期
                List<ProductPeriod> productPeriodList = productPeriodService.findByProductId(productId);
                for (ProductPeriod productPeriod1 : productPeriodList)
                {
                    boolean check = Tools.dateCheckOverlap(productPeriod.getStartTime(), productPeriod.getEndTime(), productPeriod1.getStartTime(), productPeriod1.getEndTime());
                    if (check)
                    {
                        // 被占用
                        productName.add(productPeriod.getProduct().getNumber());
                    }
                }
            }
            if (productName.size() > 0)
            {
                return ResultUtil.fail(-1, productName.toString());
            }
        }
        // 更新排期单状态
        scheduleService.updateState(id, state, request);
        return ResultUtil.success();
    }

    /**
     * 设置广告位图片
     * @param filePath
     * @param ids,
     * @param type 0 正面  1 反面
     * @return
     */
    @PostMapping(value="/vendor/schedule/product/picture")
    public Result setPicture(@RequestParam("filePath")String filePath,
                             @RequestParam("ids")String ids,
                             @RequestParam("type")Integer type)
    {

        String[] idArray = ids.split(",");
        List<Integer> idList = new ArrayList<Integer>();
        for (Integer i = 0; i < idArray.length; i ++)
        {
            idList.add(Integer.valueOf(idArray[i]));
        }

        if (type == 0)
        {
            productPeriodService.setPositivePicture(filePath, idList);
        }
        else
        {
            productPeriodService.setBackPicture(filePath, idList);
        }
        return ResultUtil.success();
    }

    /**
     * 做成排期
     * @param projectId
     * @param productIds
     * @param startTime
     * @param endTime
     * @param totalAmount
     * @param request
     * @return
     */
    @PostMapping(value="/vendor/schedule/create")
    public Result makeSchedule(@RequestParam("projectId")Integer projectId,
                               @RequestParam("productIds")String productIds,
                               @RequestParam("startTime")String startTime,
                               @RequestParam("endTime")String endTime,
                               @RequestParam("totalAmount")String totalAmount,
                               HttpServletRequest request)
    {
        try
        {

            Schedule schedule = scheduleService.makeSchedule(projectId, productIds, startTime, endTime, totalAmount, request);

            return ResultUtil.success(schedule);
        }
        catch(Exception e)
        {
            return ResultUtil.fail(-1, e.getMessage());
        }
    }

    /**
     * 删除排期单中某个产品
     * @param id
     * @param productId
     * @param scheduleId
     * @return
     */
    @DeleteMapping(value="/vendor/schedule/period/{id}/{productId}/{scheduleId}")
    public Result deleteProduct(@PathVariable("id")Integer id,
                                @PathVariable("productId")Integer productId,
                                @PathVariable("scheduleId")Integer scheduleId)
    {
        try
        {
            scheduleService.deleteProductByScheduleI(id, productId, scheduleId);
            return ResultUtil.success();
        }
        catch(Exception e)
        {
            return ResultUtil.fail(-1, e.getMessage());
        }
    }


    @PostMapping(value="/vendor/schedule/product/add")
    public Result addProductToSchedule(@RequestParam("productIds")String productIds,
                                       @RequestParam("scheduleId")Integer scheduleId,
                                       @RequestParam("startTime")String startTime,
                                       @RequestParam("endTime")String endTime)
    {
        try
        {
            scheduleService.resetSchedule(scheduleId, startTime, endTime, productIds);
            return ResultUtil.success();
        }
        catch(Exception e)
        {
            return ResultUtil.fail(-1, e.getMessage());
        }
    }
}
